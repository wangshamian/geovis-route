package org.heigit.geovisroute.routing.graphhopper.extensions;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.WeightingFactory;
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.*;
import com.graphhopper.routing.weighting.custom.CustomModelParser;
import com.graphhopper.routing.weighting.custom.CustomWeighting;
import com.graphhopper.routing.weighting.custom.CustomWeighting2;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.*;
import org.heigit.geovisroute.common.TravelRangeType;
import org.heigit.geovisroute.routing.ProfileWeighting;
import org.heigit.geovisroute.routing.RouteRequestParameterNames;
import org.heigit.geovisroute.routing.RouteSearchContext;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.TrafficGraphStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.MaximumSpeedCalculator;
import org.heigit.geovisroute.routing.graphhopper.extensions.weighting.*;
import org.heigit.geovisroute.routing.traffic.RoutingTrafficSpeedCalculator;
import org.heigit.geovisroute.util.ProfileTools;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.graphhopper.routing.weighting.TurnCostProvider.NO_TURN_COST_PROVIDER;
import static com.graphhopper.routing.weighting.Weighting.INFINITE_U_TURN_COSTS;
import static com.graphhopper.routing.weighting.custom.CustomModelParser.createWeightingParameters;
import static com.graphhopper.util.Helper.toLowerCase;
import static org.heigit.geovisroute.util.ProfileTools.VAL_CUSTOM;
import static org.heigit.geovisroute.util.ProfileTools.VAL_RECOMMENDED;

/**
 * 该类是将 GRWeightingFactory 初步适配到 GH 新版
 * WeightingFactory 接口的结果。GRWeightingFactory 是从 GH 的
 * DefaultWeightingFactory 复制并修改而来。OrsWeightingFactoryGh4
 * 旨在更清晰地在两套代码库之间处理对 DefaultWeightingFactory 的这些
 * 扩展。GRWeightingFactory 中的修改应按需迁移到 OrsWeightingFactoryGh4，
 * 以弄清 GRWeightingFactory 中哪些部分仍然需要，哪些
 * 部分已是无人维护特性的残留。
 */
public class GRWeightingFactory implements WeightingFactory {
    protected final BaseGraph graph;
    protected final EncodingManager encodingManager;

    public GRWeightingFactory(BaseGraph graph, EncodingManager encodingManager) {
        this.graph = graph;
        this.encodingManager = encodingManager;
    }

    @Override
    public Weighting createWeighting(Profile profile, PMap requestHints, boolean disableTurnCosts) {
        // 将 profile hints 与请求 hints 合并，请求 hints 优先。
        // 注意：目前我们尚未检查覆盖 profile hints 是否真的与 LM/CH 的预处理兼容。
        // 后续还应限制可用于修改 profile 的参数数量。
        PMap hints = new PMap();
        hints.putAll(profile.getHints());
        hints.putAll(requestHints);

        // ORS-GH MOD START
        // ORS 为每种权重定义一个 profile（例如 "car_ors_fastest"、"car_ors_shortest"），因此
        // 所选 profile 的权重具有决定性。原先 ORS 的 "weighting_method" hint 在此不再被处理：
        // 在 GraphHopper 11 中，ORS 各种权重的语义编码在 profile 的 CustomModel 中。
        String weightingStr = toLowerCase(profile.getWeighting());
        // ORS-GH MOD END
        if (weightingStr.isEmpty())
            throw new IllegalArgumentException("You have to specify a weighting");

        Weighting weighting = null;
        if (CustomWeighting.NAME.equalsIgnoreCase(weightingStr)) {
            final CustomModel queryCustomModel = requestHints.getObject(CustomModel.KEY, null);
            if (profile.getTurnCostsConfig() != null && !profile.getTurnCostsConfig().isAllowTurnPenaltyInRequest() && queryCustomModel != null && !queryCustomModel.getTurnPenalty().isEmpty())
                throw new IllegalArgumentException("The turn_penalty feature is not supported per request for " + profile.getName() + ". Set 'allow_turn_penalty_in_request' to true in the 'turn_costs' option in the config.yml.");

            final CustomModel mergedCustomModel = CustomModel.merge(profile.getCustomModel(), queryCustomModel);
            if (requestHints.has(Parameters.Routing.HEADING_PENALTY))
                mergedCustomModel.setHeadingPenalty(requestHints.getDouble(Parameters.Routing.HEADING_PENALTY, Parameters.Routing.DEFAULT_HEADING_PENALTY));

            CustomWeighting.Parameters parameters = createWeightingParameters(mergedCustomModel, encodingManager);
            final TurnCostProvider turnCostProvider;
            if (profile.hasTurnCosts() && !disableTurnCosts) {
                BooleanEncodedValue turnRestrictionEnc = encodingManager.getTurnBooleanEncodedValue(TurnRestriction.key(profile.getName()));
                if (turnRestrictionEnc == null)
                    throw new IllegalArgumentException("Cannot find turn restriction encoded value for " + profile.getName());
                int uTurnCosts = hints.getInt(Parameters.Routing.U_TURN_COSTS, profile.getTurnCostsConfig().getUTurnCosts());
                TurnCostsConfig tcConfig = new TurnCostsConfig(profile.getTurnCostsConfig()).setUTurnCosts(uTurnCosts);
                turnCostProvider = new DefaultTurnCostProvider(turnRestrictionEnc, graph, tcConfig, parameters.getTurnPenaltyMapping());
            } else {
                if (!mergedCustomModel.getTurnPenalty().isEmpty())
                    throw new IllegalArgumentException("The turn_penalty feature is not supported for " + profile.getName() + ". You have to enable this in 'turn_costs' in config.yml.");
                turnCostProvider = NO_TURN_COST_PROVIDER;
            }

            // ORS 的 profile 名称带有权重后缀（例如 "car_ors_fastest"），因此无法像
            // GraphHopper 的 DefaultWeightingFactory 那样从 profile 名称推导速度编码值。改用 profile 的车辆。
            EncodedValue encodedValue = encodingManager.getEncodedValue(VehicleSpeed.key(getVehicle(profile)), EncodedValue.class);
            if (hints.has("cm_version")) {
                if (!hints.getString("cm_version", "").equals("2"))
                    throw new IllegalArgumentException("cm_version: \"2\" is required");
                weighting = new CustomWeighting2(encodedValue, turnCostProvider, parameters);
            } else {
                weighting = new CustomWeighting(encodedValue, turnCostProvider, parameters);
            }

        } else if ("shortest".equalsIgnoreCase(weightingStr)) {
            throw new IllegalArgumentException("Instead of weighting=shortest use weighting=custom with a high distance_influence");
        } else if ("fastest".equalsIgnoreCase(weightingStr)) {
            throw new IllegalArgumentException("Instead of weighting=fastest use weighting=custom with a custom model that avoids road_access == DESTINATION");
        } else if ("curvature".equalsIgnoreCase(weightingStr)) {
            throw new IllegalArgumentException("The curvature weighting is no longer supported since 7.0. Use a custom " +
                    "model with the EncodedValue 'curvature' instead");
        } else if ("short_fastest".equalsIgnoreCase(weightingStr)) {
            throw new IllegalArgumentException("Instead of weighting=short_fastest use weighting=custom with a distance_influence");
        }

        if (weighting == null)
            throw new IllegalArgumentException("Weighting '" + weightingStr + "' not supported");

        return weighting;
    }

    /**
     * 确定 profile 所属的 GraphHopper 11 车辆。ORS 的 profile 名称为
     * {@code <ors-vehicle>_<weighting>[_with_turn_costs]}（例如 "car_ors_fastest"），而 GraphHopper
     * 编码值使用的车辆名称更短（例如 "car"）。由于 ORS 每张图只构建一种车辆，可直接使用
     * 唯一注册的车辆，否则取名称作为 profile 名称前缀的车辆。
     */
    private String getVehicle(Profile profile) {
        List<String> vehicles = encodingManager.getVehicles();
        if (vehicles.size() == 1)
            return vehicles.get(0);
        return vehicles.stream()
                .filter(v -> profile.getName().startsWith(v + "_"))
                .max(Comparator.comparingInt(String::length))
                .orElseThrow(() -> new IllegalArgumentException("Cannot determine the vehicle of profile '" + profile.getName() + "'"));
    }

    /**
     * 视情况将指定的权重包装为 TimeDependentAccessWeighting。
     */
//    private Weighting createTimeDependentAccessWeighting(Weighting weighting) {
//        FlagEncoder flagEncoder = weighting.getFlagEncoder();
//        if (encodingManager.hasEncodedValue(EncodingManager.getKey(flagEncoder, ConditionalEdges.ACCESS)))
//            return new TimeDependentAccessWeighting(weighting, ghStorage, flagEncoder);
//        else
//            return weighting;
//    }

//    public Weighting createIsochroneWeighting(Profile profile) {
//        FlagEncoder encoder = this.encodingManager.getEncoder(profile.get());
//        String weightingStr = toLowerCase(profile.getWeighting());
//        Weighting result = null;
//
//        //Isochrones only support fastest or shortest as no path is found.
//        //CalcWeight must be directly comparable to the isochrone limit
//
//        if ("shortest".equalsIgnoreCase(weightingStr)) {
//            result = new ShortestWeighting(encoder);
//        } else if ("fastest".equalsIgnoreCase(weightingStr)
//                || "priority".equalsIgnoreCase(weightingStr)
//                || "recommended_pref".equalsIgnoreCase(weightingStr)
//                || "recommended".equalsIgnoreCase(weightingStr)) {
//            result = new GRFastestWeighting(encoder);
//        }
//
//        return result;
//    }

    /**
     * 创建等时圈使用的权重。它必须基于图中真实的速度编码值：原先临时创建的
     * {@code new DecimalEncodedValueImpl(vehicleName, ...)} 既没有有效的位位置，也没有
     * GraphHopper 11 使用的名称（{@code <vehicle>_average_speed}），因此边的速度是从错误的位上读取的。
     */
    public static Weighting createIsochroneWeighting(RouteSearchContext searchContext, EncodingManager encodingManager) {
        DecimalEncodedValue speedEnc = getSpeedEncodedValue(encodingManager, searchContext.getVehicleName());
        return new GRFastestWeighting(speedEnc);
    }

    /**
     * 解析给定 ORS 车辆 "fastest" profile 的子网络编码值。GraphHopper 11 会按
     * profile 名称创建这些编码值，例如 {@code car_ors_fastest_subnetwork}。
     */
    public static BooleanEncodedValue getSubnetworkEncodedValue(EncodingManager encodingManager, String orsVehicleName) {
        for (boolean turnCosts : new boolean[]{false, true}) {
            String key = Subnetwork.key(ProfileTools.makeProfileName(orsVehicleName, ProfileTools.VAL_FASTEST, turnCosts));
            if (encodingManager.hasEncodedValue(key))
                return encodingManager.getBooleanEncodedValue(key);
        }
        throw new IllegalArgumentException("Cannot find the subnetwork encoded value of profile '" +
                ProfileTools.makeProfileName(orsVehicleName, ProfileTools.VAL_FASTEST, false) + "'");
    }

    private static DecimalEncodedValue getSpeedEncodedValue(EncodingManager encodingManager, String orsVehicleName) {
        String ghVehicle = FlagEncoderNames.getGHVehicleName(orsVehicleName);
        return encodingManager.getDecimalEncodedValue(VehicleSpeed.key(ghVehicle));
    }

//    protected void setSpeedCalculator(Weighting weighting, PMap requestHints) {
//        if (isRequestTimeDependent(requestHints)) {
//            // OSM conditionals
//            FlagEncoder encoder = weighting.getFlagEncoder();
//            if (encodingManager.hasEncodedValue(EncodingManager.getKey(encoder, ConditionalEdges.SPEED)))
//                weighting.setSpeedCalculator(new ConditionalSpeedCalculator(weighting.getSpeedCalculator(), ghStorage, encoder));
//
//            // traffic data
//            Instant time = requestHints.getObject(requestHints.has(RouteRequestParameterNames.PARAM_DEPARTURE) ?
//                    RouteRequestParameterNames.PARAM_DEPARTURE : RouteRequestParameterNames.PARAM_ARRIVAL, null);
//            addTrafficSpeedCalculator(weighting, ghStorage, time);
//        }
//
//        if (requestHints.has("maximum_speed")) {
//            double maximumSpeedLowerBound = requestHints.getDouble("maximum_speed_lower_bound", 0);
//            double maximumSpeed = requestHints.getDouble("maximum_speed", maximumSpeedLowerBound);
//            weighting.setSpeedCalculator(new MaximumSpeedCalculator(weighting.getSpeedCalculator(), maximumSpeed));
//        }
//    }

    protected boolean isRequestTimeDependent(PMap hints) {
        return hints.has(RouteRequestParameterNames.PARAM_DEPARTURE) || hints.has(RouteRequestParameterNames.PARAM_ARRIVAL);
    }

//    protected Weighting applySoftWeightings(PMap hints, FlagEncoder encoder, Weighting weighting) {
//        // TODO (cleanup): The term "custom_weighting" is easily confused with GH's custom
//        //                 weighting and should be renamed.
//        if (hints.getBool("custom_weightings", false)) {
//            Map<String, Object> map = hints.toMap();
//
//            List<String> weightingNames = new ArrayList<>();
//            for (Map.Entry<String, Object> kv : map.entrySet()) {
//                String name = ProfileWeighting.decodeName(kv.getKey());
//                if (name != null && !weightingNames.contains(name))
//                    weightingNames.add(name);
//            }
//
//            List<Weighting> softWeightings = new ArrayList<>();
//
//            for (String weightingName : weightingNames) {
//                switch (weightingName) {
//                    case "steepness_difficulty" ->
//                            softWeightings.add(new SteepnessDifficultyWeighting(encoder, getWeightingProps(weightingName, map), ghStorage));
//                    case "avoid_hills" ->
//                            softWeightings.add(new AvoidHillsWeighting(encoder, getWeightingProps(weightingName, map), ghStorage));
//                    case "green" ->
//                            softWeightings.add(new GreenWeighting(encoder, getWeightingProps(weightingName, map), ghStorage));
//                    case "quiet" ->
//                            softWeightings.add(new QuietWeighting(encoder, getWeightingProps(weightingName, map), ghStorage));
//                    case "csv" ->
//                            softWeightings.add(new HeatStressWeighting(encoder, getWeightingProps(weightingName, map), ghStorage));
//                    case "shadow" ->
//                            softWeightings.add(new ShadowWeighting(encoder, getWeightingProps(weightingName, map), ghStorage));
//                    default -> {
//                    }
//                }
//            }
//
//            if (!softWeightings.isEmpty()) {
//                weighting = new AdditionWeighting(softWeightings, weighting);
//            }
//        }
//        return weighting;
//    }

    private PMap getWeightingProps(String weightingName, Map<String, Object> map) {
        PMap res = new PMap();

        String prefix = "weighting_#" + weightingName;
        int n = prefix.length();

        for (Map.Entry<String, Object> kv : map.entrySet()) {
            String name = kv.getKey();
            int p = name.indexOf(prefix);
            if (p >= 0)
                res.putObject(name.substring(p + n + 1), kv.getValue());
        }

        return res;
    }

    public static void addTrafficSpeedCalculator(List<Weighting> weightings, BaseGraph baseGraph) {
        for (Weighting weighting : weightings)
            addTrafficSpeedCalculator(weighting, baseGraph, null);
    }

    private static void addTrafficSpeedCalculator(Weighting weighting, BaseGraph baseGraph, Instant time) {
        TrafficGraphStorage trafficGraphStorage = GraphStorageUtils.getGraphExtension(baseGraph, TrafficGraphStorage.class);

        if (trafficGraphStorage != null) {
            RoutingTrafficSpeedCalculator routingTrafficSpeedCalculator = new RoutingTrafficSpeedCalculator(weighting.getSpeedCalculator(), baseGraph, weighting.getName());

            if (time != null) {
                //使用固定时区，因为原始实现是针对德国交通数据的
                ZonedDateTime zdt = time.atZone(ZoneId.of("Europe/Berlin"));
                routingTrafficSpeedCalculator.setZonedDateTime(zdt);
            }

            weighting.setSpeedCalculator(routingTrafficSpeedCalculator);
        }
    }
}
