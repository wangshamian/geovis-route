package org.heigit.geovisroute.util;

import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.ConditionalEdges;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.exceptions.InternalServerException;
import org.heigit.geovisroute.routing.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;
import org.heigit.geovisroute.routing.parameters.ProfileParameters;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * 该类只是重构期间用于临时存放缺少合适归属位置的辅助方法。
 * TODO: 重构完成后，应将这些方法移动到合适的位置，并删除该类。
 */
public class TemporaryUtilShelter {

    public static boolean supportWeightingMethod(int profileType) {
        return RoutingProfileType.isDriving(profileType) || RoutingProfileType.isCycling(profileType) || RoutingProfileType.isPedestrian(profileType);
    }

    /**
     * 获取 searchParams 所需的灵活模式。
     * 结果将决定使用 CH、Core 还是 ALT/AStar。
     *
     * @param flexibleMode 初始的 flexibleMode
     * @param searchParams RouteSearchParameters
     * @param profileType  对 HGV 而言是必需的
     * @return 以 int 表示的灵活性
     */
    public static int getFlexibilityMode(int flexibleMode, RouteSearchParameters searchParams, int profileType) {
        if (searchParams.requiresDynamicPreprocessedWeights() || profileType == RoutingProfileType.WHEELCHAIR)
            flexibleMode = ProfileTools.KEY_FLEX_PREPROCESSED;

        if (searchParams.requiresFullyDynamicWeights())
            flexibleMode = ProfileTools.KEY_FLEX_FULLY;
        //如果有特殊权重，则必须回退到带 Beeline 的 ALT
        ProfileParameters profileParams = searchParams.getProfileParameters();
        if (profileParams != null && profileParams.hasWeightings())
            flexibleMode = ProfileTools.KEY_FLEX_FULLY;

        return flexibleMode;
    }

    public static boolean hasTimeDependentSpeed(RouteSearchParameters searchParams, RouteSearchContext searchCntx) {
        String vehicleName = searchCntx.getVehicleName();
        String key = EncodingManager.getKey(vehicleName, ConditionalEdges.SPEED);
        return searchParams.isTimeDependent() && searchCntx.getGraphHopper().getEncodingManager().hasEncodedValue(key);
    }

    public static RouteSearchContext createSearchContext(RouteSearchParameters searchParams, RoutingProfile routingProfile) throws InternalServerException {
        PMap props = new PMap();

        int profileType = searchParams.getProfileType();
        String vehicleName = RoutingProfileType.getEncoderName(profileType);

        if (FlagEncoderNames.UNKNOWN.equals(vehicleName))
            throw new InternalServerException(RoutingErrorCodes.UNKNOWN, "unknown vehicle profile.");

        // GraphHopper 11 通过 "<vehicle>_access" 编码车辆，而 ORS 仍使用自己的 encoder 名称
        // （例如 "car_ors"）。因此需要检查映射后的 GraphHopper vehicle 的 access 编码值。
        if (!routingProfile.getGraphhopper().getEncodingManager().hasEncodedValue(VehicleAccess.key(FlagEncoderNames.getGHVehicleName(vehicleName)))) {
            throw new IllegalArgumentException("Vehicle " + vehicleName + " unsupported. " + "Supported are: "
                    + routingProfile.getGraphhopper().getEncodingManager());
        }

//        TagParser tagParser = routingProfile.getGraphhopper().getEncodingManager().getBooleanEncodedValue(encoderName);
        ProfileParameters profileParams = searchParams.getProfileParameters();

        // PathProcessorFactory 的参数

        props.putObject("routing_extra_info", searchParams.getExtraInfo());
        props.putObject("routing_suppress_warnings", searchParams.getSuppressWarnings());

        props.putObject("routing_profile_type", profileType);
        props.putObject("routing_profile_params", profileParams);

        /*
         * EdgeFilterFactory 的参数
         * ======================================================================================================
         */

        /* 避让区域 */
        if (searchParams.hasAvoidAreas()) {
            props.putObject("avoid_areas", searchParams.getAvoidAreas());
        }

        /* 重型车辆过滤器 */
        if (profileType == RoutingProfileType.DRIVING_HGV || profileType == RoutingProfileType.DRIVING_MILITARY) {
            props.putObject("edgefilter_hgv", searchParams.getVehicleType());
        }

        /* 轮椅过滤器 */
        else if (profileType == RoutingProfileType.WHEELCHAIR) {
            props.putObject("edgefilter_wheelchair", "true");
        }

        /* 避让地物 */
        if (searchParams.hasAvoidFeatures()) {
            props.putObject("avoid_features", searchParams);
        }

        /* 避让某种形式的边界 */
        if ((searchParams.hasAvoidBorders() || searchParams.hasAvoidCountries())
                && (RoutingProfileType.isDriving(profileType) || RoutingProfileType.isCycling(profileType))) {
            props.putObject("avoid_borders", searchParams);
            if (searchParams.hasAvoidCountries())
                props.putObject("avoid_countries", Arrays.toString(searchParams.getAvoidCountries()));
        }

        if (profileParams != null && profileParams.hasWeightings()) {
            props.putObject(ProfileTools.KEY_CUSTOM_WEIGHTINGS, true);
            Iterator<ProfileWeighting> iterator = profileParams.getWeightings().getIterator();
            while (iterator.hasNext()) {
                ProfileWeighting weighting = iterator.next();
                if (!weighting.getParameters().isEmpty()) {
                    String name = ProfileWeighting.encodeName(weighting.getName());
                    for (Map.Entry<String, Object> kv : weighting.getParameters().toMap().entrySet())
                        props.putObject(name + kv.getKey(), kv.getValue());
                }
            }
        }

        String effectiveWeightingMethod = searchParams.getCustomModel() != null ? WeightingMethod.getName(WeightingMethod.CUSTOM) : WeightingMethod.getName(searchParams.getWeightingMethod());
        String localProfileName = ProfileTools.makeProfileName(vehicleName, effectiveWeightingMethod,
                Boolean.TRUE.equals(routingProfile.getProfileProperties().getBuild().getEncoderOptions().getTurnCosts()));
        String profileNameCH = ProfileTools.makeProfileName(vehicleName, WeightingMethod.getName(searchParams.getWeightingMethod()), false);
        RouteSearchContext searchCntx = new RouteSearchContext(routingProfile.getGraphhopper(), vehicleName, localProfileName, profileNameCH);
        searchCntx.setProperties(props);

        return searchCntx;
    }
}
