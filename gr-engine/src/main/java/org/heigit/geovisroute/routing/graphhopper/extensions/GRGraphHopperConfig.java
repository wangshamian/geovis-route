package org.heigit.geovisroute.routing.graphhopper.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.LMProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.json.Statement;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.config.ElevationProperties;
import org.heigit.geovisroute.config.EngineProperties;
import org.heigit.geovisroute.config.profile.BuildProperties;
import org.heigit.geovisroute.config.profile.ExecutionProperties;
import org.heigit.geovisroute.config.profile.PreparationProperties;
import org.heigit.geovisroute.config.profile.ProfileProperties;
import org.heigit.geovisroute.routing.RoutingProfileType;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GRParameters;
import org.heigit.geovisroute.util.ProfileTools;
import org.heigit.geovisroute.util.StringUtility;

import java.nio.file.Path;
import java.util.*;

import org.apache.log4j.Logger;

public class GRGraphHopperConfig extends GraphHopperConfig {
    private List<CHProfile> coreProfiles = new ArrayList<>();
    private List<LMProfile> coreLMProfiles = new ArrayList<>();
    private List<Profile> fastisochroneProfiles = new ArrayList<>();
    private static final Logger LOGGER = org.apache.log4j.Logger.getLogger(GRGraphHopperConfig.class.getName());

    /**
     * 仅根据引擎高程配置创建 ElevationProvider，不写入路网图。
     * 供越野路径等按需查询高程的功能使用，避免把现有 2D 图重建为 3D 图。
     */
    public static ElevationProvider createElevationProvider(ElevationProperties elevationProps) {
        if (elevationProps == null || elevationProps.getProvider() == null || elevationProps.getCachePath() == null) {
            LOGGER.warn("Elevation provider or cache path is null. Elevation deactivated.");
            return ElevationProvider.NOOP;
        }
        GRGraphHopperConfig ghConfig = new GRGraphHopperConfig();
        ghConfig.putObject("graph.elevation.provider", StringUtility.trimQuotes(elevationProps.getProvider()));
        ghConfig.putObject("graph.elevation.cache_dir", StringUtility.trimQuotes(elevationProps.getCachePath().toString()));
        if (elevationProps.getDataAccess() != null) {
            ghConfig.putObject("graph.elevation.dataaccess", StringUtility.trimQuotes(elevationProps.getDataAccess().toString()));
        }
        if (elevationProps.getCacheClear() != null) {
            ghConfig.putObject("graph.elevation.clear", elevationProps.getCacheClear());
        }
        return GraphHopper.createElevationProvider(ghConfig);
    }

    public static GRGraphHopperConfig createGHSettings(ProfileProperties profile, EngineProperties engineConfig, String graphLocation) {
        GRGraphHopperConfig ghConfig = new GRGraphHopperConfig();
        ghConfig.putObject("graph.dataaccess", engineConfig.getGraphsDataAccess().toString());
        ghConfig.putObject("datareader.file", Optional.ofNullable(profile).map(ProfileProperties::getBuild).map(BuildProperties::getSourceFile).map(Path::toString).orElse(null));
        ghConfig.putObject("graph.bytes_for_flags", profile.getBuild().getEncoderFlagsSize());
        ghConfig.putObject("graph.location", graphLocation);
        // GraphHopper 11 要求显式设置该选项，空值表示“不跳过任何 highway 类型”。
        ghConfig.putObject("import.osm.ignored_highways", "");

        if (Boolean.FALSE.equals(profile.getBuild().getInstructions())) {
            ghConfig.putObject("instructions", false);
        }

        ElevationProperties elevationProps = engineConfig.getElevation();

        boolean addElevation = true;

    
        if (!profile.getBuild().getElevation()) {
            LOGGER.warn("Elevation is set to false.");
            addElevation = false;
        }
        if (elevationProps.getProvider() == null) {
            LOGGER.warn("Elevation provider is null.");
            addElevation = false;
        }
        if (elevationProps.getCachePath() == null) {
            LOGGER.warn("Elevation cache path is null.");
            addElevation = false;
        }
        
        // GraphHopper 11 不再支持旧的 "graph.flag_encoders" 选项。车辆本身通过下面的
        // Profile 对象配置，而图导入需要从 OSM 读取的所有内容则通过
        // "graph.encoded_values" 声明。
        Set<String> encodedValues = new LinkedHashSet<>();

        if (!addElevation) {
             LOGGER.warn("Elevation deactivated.");
        } else {
            ghConfig.putObject("graph.elevation.provider", StringUtility.trimQuotes(elevationProps.getProvider()));
            ghConfig.putObject("graph.elevation.cache_dir", StringUtility.trimQuotes(elevationProps.getCachePath().toString()));
            ghConfig.putObject("graph.elevation.dataaccess", StringUtility.trimQuotes(elevationProps.getDataAccess().toString()));
            ghConfig.putObject("graph.elevation.clear", elevationProps.getCacheClear());
            if (Boolean.TRUE.equals(profile.getBuild().getInterpolateBridgesAndTunnels()))
                encodedValues.add("road_environment");
            if (Boolean.TRUE.equals(profile.getBuild().getElevationSmoothing()))
                ghConfig.putObject("graph.elevation.smoothing", true);
        }

        boolean prepareCH = false;
        boolean prepareLM = false;
        boolean prepareCore = false;
        boolean prepareFI = false;

        Integer[] profilesTypes = profile.getProfilesTypes();
        Map<String, Profile> profiles = new LinkedHashMap<>();

        // TODO 后续改进：过去曾使用多个 profile 来为多个
        //       bike profile 共享同一张图。目前未使用该特性，但将来
        //       可能有需求。不过，在原生 GH 中通过已有的机制
        //       这是标准做法。
        if (profilesTypes.length != 1)
            throw new IllegalStateException("Expected single profile in config");

        String vehicle = RoutingProfileType.getEncoderName(profilesTypes[0]);
        String ghVehicle = getGHVehicleName(vehicle);

        boolean hasTurnCosts = Boolean.TRUE.equals(profile.getBuild().getEncoderOptions().getTurnCosts());

        // TODO 后续改进：像 GH 中那样，为每种车辆分别配置该权重列表
        String[] weightings = {ProfileTools.VAL_FASTEST, ProfileTools.VAL_SHORTEST, ProfileTools.VAL_RECOMMENDED};
        for (String weighting : weightings) {
            if (hasTurnCosts) {
                String profileName = ProfileTools.makeProfileName(vehicle, weighting, true);
                profiles.put(profileName, createProfile(profileName, weighting, ghVehicle));
            }
            String profileName = ProfileTools.makeProfileName(vehicle, weighting, false);
            profiles.put(profileName, createProfile(profileName, weighting, ghVehicle));
        }

        if (Boolean.TRUE == profile.getBuild().getEncoderOptions().getEnableCustomModels()) {
            if (hasTurnCosts)
                profiles.put(vehicle + "_custom_with_turn_costs", createProfile(vehicle + "_custom_with_turn_costs", ProfileTools.VAL_FASTEST, ghVehicle));
            profiles.put(vehicle + "_custom", createProfile(vehicle + "_custom", ProfileTools.VAL_FASTEST, ghVehicle));
        }

        ghConfig.putObject(ProfileTools.KEY_PREPARE_CORE_WEIGHTINGS, "no");
        if (profile.getBuild().getPreparation() != null) {
            PreparationProperties preparations = profile.getBuild().getPreparation();


            if (preparations.getMinNetworkSize() != null)
                ghConfig.putObject("prepare.min_network_size", preparations.getMinNetworkSize());

            if (!preparations.getMethods().isEmpty()) {
                if (!preparations.getMethods().getCh().isEmpty()) {
                    PreparationProperties.MethodsProperties.CHProperties chOpts = preparations.getMethods().getCh();
                    prepareCH = Boolean.TRUE.equals(chOpts.getEnabled());
                    if (prepareCH) {
                        if (chOpts.getThreadsSave() != null)
                            ghConfig.putObject("prepare.ch.threads", chOpts.getThreadsSave());
                        if (chOpts.getWeightings() != null) {
                            List<CHProfile> chProfiles = new ArrayList<>();
                            String chWeightingsString = StringUtility.trimQuotes(chOpts.getWeightings());
                            for (String weighting : chWeightingsString.split(","))
                                chProfiles.add(new CHProfile(ProfileTools.makeProfileName(vehicle, weighting, false)));
                            ghConfig.setCHProfiles(chProfiles);
                        }
                    }
                }

                if (!preparations.getMethods().getLm().isEmpty()) {
                    PreparationProperties.MethodsProperties.LMProperties lmOpts = preparations.getMethods().getLm();
                    prepareLM = Boolean.TRUE.equals(lmOpts.getEnabled());
                    if (prepareLM) {
                        if (lmOpts.getThreadsSave() != null)
                            ghConfig.putObject("prepare.lm.threads", lmOpts.getThreadsSave());
                        if (lmOpts.getWeightings() != null) {
                            List<LMProfile> lmProfiles = new ArrayList<>();
                            String lmWeightingsString = StringUtility.trimQuotes(lmOpts.getWeightings());
                            for (String weighting : lmWeightingsString.split(","))
                                lmProfiles.add(new LMProfile(ProfileTools.makeProfileName(vehicle, weighting, hasTurnCosts)));
                            ghConfig.setLMProfiles(lmProfiles);
                        }
                        if (lmOpts.getLandmarks() != null)
                            ghConfig.putObject("prepare.lm.landmarks", lmOpts.getLandmarks());
                    }
                }

                if (!preparations.getMethods().getCore().isEmpty()) {
                    PreparationProperties.MethodsProperties.CoreProperties coreOpts = preparations.getMethods().getCore();
                    prepareCore = Boolean.TRUE.equals(coreOpts.getEnabled());
                    if (prepareCore) {
                        if (coreOpts.getThreadsSave() != null) {
                            Integer threadsCore = coreOpts.getThreadsSave();
                            ghConfig.putObject("prepare.core.threads", threadsCore);
                            ghConfig.putObject("prepare.corelm.threads", threadsCore);
                        }
                        if (coreOpts.getWeightings() != null) {
                            String coreWeightingsString = StringUtility.trimQuotes(coreOpts.getWeightings());
                            for (String weighting : coreWeightingsString.split(",")) {
                                String configStr = "";
                                if (weighting.contains("|")) {
                                    configStr = weighting;
                                    weighting = weighting.split("\\|")[0];
                                }
                                PMap configMap = new PMap(configStr);
                                boolean considerTurnRestrictions = configMap.getBool("edge_based", hasTurnCosts);

                                String profileName = ProfileTools.makeProfileName(vehicle, weighting, considerTurnRestrictions);
                                profiles.put(profileName, createProfile(profileName, weighting, ghVehicle));
                            }
                            // ORS 的 core 预处理尚未移植到该 GraphHopper 11 集成中：core 的
                            // CH 配置从未创建，因此注册 core profile 会使 GraphHopper 在后处理阶段
                            // 因 "No CH profiles found" 而失败。profile 本身保留，
                            // 因为路径计算仍在使用它们，仅跳过 core 预处理。
                            LOGGER.warn("Core preparation is not supported by this GraphHopper version, skipping it.");
                        }
                        if (coreOpts.getLmsets() != null)
                            ghConfig.putObject("prepare.corelm.lmsets", StringUtility.trimQuotes(coreOpts.getLmsets()));
                        if (coreOpts.getLandmarks() != null)
                            ghConfig.putObject("prepare.corelm.landmarks", coreOpts.getLandmarks());
                    } else {
                        ghConfig.putObject(ProfileTools.KEY_PREPARE_CORE_WEIGHTINGS, "no");
                    }
                }

                if (!preparations.getMethods().getFastisochrones().isEmpty()) {
                    PreparationProperties.MethodsProperties.FastIsochroneProperties fastisochroneOpts = preparations.getMethods().getFastisochrones();
                    prepareFI = Boolean.TRUE.equals(fastisochroneOpts.getEnabled());
                    if (prepareFI) {
                        ghConfig.putObject(GRParameters.FastIsochrone.PROFILE, profile.getEncoderName().toString());
                        //从 core 复制而来
                        if (fastisochroneOpts.getThreadsSave() != null)
                            ghConfig.putObject("prepare.fastisochrone.threads", fastisochroneOpts.getThreadsSave());
                        if (fastisochroneOpts.getMaxcellnodes() != null)
                            ghConfig.putObject("prepare.fastisochrone.maxcellnodes", fastisochroneOpts.getMaxcellnodes());
                        if (fastisochroneOpts.getWeightings() != null) {
                            List<Profile> fastisochronesProfiles = new ArrayList<>();
                            String fastisochronesWeightingsString = StringUtility.trimQuotes(fastisochroneOpts.getWeightings());
                            for (String weighting : fastisochronesWeightingsString.split(",")) {
                                String configStr = "";
                                weighting = weighting.trim();
                                if (weighting.contains("|")) {
                                    configStr = weighting;
                                    weighting = weighting.split("\\|")[0];
                                }
                                PMap configMap = new PMap(configStr);
                                boolean considerTurnRestrictions = configMap.getBool("edge_based", hasTurnCosts);

                                String profileName = ProfileTools.makeProfileName(vehicle, weighting, considerTurnRestrictions);
                                Profile ghProfile = createProfile(profileName, weighting, ghVehicle);
                                profiles.put(profileName, ghProfile);
                                fastisochronesProfiles.add(ghProfile);
                            }
                            ghConfig.setFastisochroneProfiles(fastisochronesProfiles);
                        }
                    } else {
                        ghConfig.putObject(ProfileTools.KEY_PREPARE_FASTISOCHRONE_WEIGHTINGS, "no");
                    }
                }
            }
        }

        if (profile.getService().getExecution() != null) {
            ExecutionProperties execution = profile.getService().getExecution();
            if (!execution.getMethods().getCore().isEmpty() && execution.getMethods().getCore().getActiveLandmarks() != null)
                ghConfig.putObject("routing.corelm.active_landmarks", execution.getMethods().getCore().getActiveLandmarks());

            if (!execution.getMethods().getLm().isEmpty() && execution.getMethods().getLm().getActiveLandmarks() != null)
                ghConfig.putObject("routing.lm.active_landmarks", execution.getMethods().getLm().getActiveLandmarks());
        }

        if (Boolean.TRUE.equals(profile.getBuild().getOptimize()) && !prepareCH)
            ghConfig.putObject("graph.do_sort", true);

        // 检查 getGTFSFile 是否存在
        if (profile.getBuild().getGtfsFile() != null && !profile.getBuild().getGtfsFile().toString().isEmpty())
            ghConfig.putObject("gtfs.file", profile.getBuild().getGtfsFile().toAbsolutePath().toString());

        encodedValues.addAll(getEncodedValuesForVehicle(vehicle));
        ghConfig.putObject("graph.encoded_values", String.join(",", encodedValues));
        ghConfig.putObject("index.high_resolution", profile.getBuild().getLocationIndexResolution());
        ghConfig.putObject("index.max_region_search", profile.getBuild().getLocationIndexSearchIterations());
        ghConfig.setProfiles(new ArrayList<>(profiles.values()));

        return ghConfig;
    }

    /**
     * 用于 "shortest" profile 的权重。GraphHopper 11 不再有专门的 shortest 权重，因此
     * 通过一个距离影响非常大（秒每公里）的自定义权重来建模。
     */
    private static final double SHORTEST_DISTANCE_INFLUENCE = 3600.0;

    /**
     * 创建 GraphHopper 11 profile。由于 GH11 仅支持 {@code custom} 权重，ORS 的权重
     * （fastest/recommended/shortest）通过 profile 的 {@link CustomModel} 表达：边的速度
     * 被限制为该 profile 车辆的平均速度，并且对于 "shortest" 会加入较大的距离影响。
     */
    private static Profile createProfile(String profileName, String weighting, String ghVehicle) {
        CustomModel customModel = new CustomModel()
                .addToSpeed(Statement.If("true", Statement.Op.LIMIT, VehicleSpeed.key(ghVehicle)));
        if (ProfileTools.VAL_SHORTEST.equals(weighting))
            customModel.setDistanceInfluence(SHORTEST_DISTANCE_INFLUENCE);
        return new Profile(profileName).setWeighting(ProfileTools.VAL_CUSTOM).setCustomModel(customModel);
    }

    /**
     * 将 ORS 的车辆/encoder 名称（参见 {@code FlagEncoderNames}）映射为对应 GraphHopper 11
     * 车辆的名称。在 GH11 中，车辆由其 {@code <vehicle>_access} 和 {@code <vehicle>_average_speed}
     * 编码值表示，这些编码值必须在 {@code graph.encoded_values} 中声明。
     */
    private static String getGHVehicleName(String vehicle) {
        return FlagEncoderNames.getGHVehicleName(vehicle);
    }

    private static List<String> getEncodedValuesForVehicle(String vehicle) {
        String ghVehicle = getGHVehicleName(vehicle);

        List<String> encodedValues = new ArrayList<>();
        encodedValues.add(ghVehicle + "_access");
        encodedValues.add(ghVehicle + "_average_speed");

        // 并非由车辆本身隐含的 ORS 专用编码值
        switch (vehicle.toLowerCase()) {
            case "heavyvehicle" -> {
                encodedValues.add("hgv");
                // 限高/限宽/限重等属性存放在这些编码值中，custom_roads 的 overlay 模式
                // 需要它们才能按几何覆盖已有 OSM 边的限制属性。
                encodedValues.addAll(List.of("max_height", "max_width", "max_weight", "max_length", "max_axle_load"));
            }
            case "hiking_ors", "hike" -> encodedValues.add("hike_rating");
            default -> {
            }
        }

        return encodedValues;
    }

    public List<CHProfile> getCoreProfiles() {
        return coreProfiles;
    }

    public GraphHopperConfig setCoreProfiles(List<CHProfile> coreProfiles) {
        this.coreProfiles = coreProfiles;
        return this;
    }

    public List<LMProfile> getCoreLMProfiles() {
        return coreLMProfiles;
    }

    @JsonProperty("prepare.corelm.threads")
    public GraphHopperConfig setCoreLMProfiles(List<LMProfile> coreLMProfiles) {
        this.coreLMProfiles = coreLMProfiles;
        return this;
//        String coreLmThreadsKey = "prepare.corelm.threads";
//        if (has(coreLmThreadsKey))
//            putObject(coreLmThreadsKey, getInt(coreLmThreadsKey, 1));
    }

    public List<Profile> getFastisochroneProfiles() {
        return fastisochroneProfiles;
    }

    public GraphHopperConfig setFastisochroneProfiles(List<Profile> fastisochroneProfiles) {
        this.fastisochroneProfiles = fastisochroneProfiles;
        return this;
    }
}
