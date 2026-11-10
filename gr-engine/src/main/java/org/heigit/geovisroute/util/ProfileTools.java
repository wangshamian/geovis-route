package org.heigit.geovisroute.util;

import com.graphhopper.util.PMap;
import org.heigit.geovisroute.routing.RoutingProfileType;
import org.heigit.geovisroute.routing.WeightingMethod;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GRParameters;

public class ProfileTools {
    public static final String VAL_RECOMMENDED = "recommended";
    public static final String VAL_CUSTOM = "custom";
    private static final String KEY_WEIGHTING = "weighting";
    private static final String KEY_WEIGHTING_METHOD = "weighting_method";
    public static final String KEY_CH_DISABLE = "ch.disable";
    public static final String KEY_LM_DISABLE = "lm.disable";
    public static final String KEY_CORE_DISABLE = "core.disable";
    public static final String KEY_PREPARE_CORE_WEIGHTINGS = "prepare.core.weightings";
    public static final String KEY_PREPARE_FASTISOCHRONE_WEIGHTINGS = "prepare.fastisochrone.weightings";
    public static final String KEY_METHODS_CH = "methods.ch";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_THREADS = "threads";
    public static final String KEY_WEIGHTINGS = "weightings";
    public static final String KEY_LMSETS = "lmsets";
    public static final String KEY_MAXCELLNODES = "maxcellnodes";
    public static final String KEY_METHODS_LM = "methods.lm";
    public static final String KEY_LANDMARKS = "landmarks";
    public static final String KEY_METHODS_CORE = "methods.core";
    public static final String KEY_ACTIVE_LANDMARKS = "active_landmarks";
    public static final String KEY_TOTAL_POP = "total_pop";
    public static final String KEY_TOTAL_AREA_KM = "total_area_km";
    public static final int KEY_FLEX_STATIC = 0;
    public static final int KEY_FLEX_PREPROCESSED = 1;
    public static final int KEY_FLEX_FULLY = 2;
    public static final String KEY_CUSTOM_WEIGHTINGS = "custom_weightings";
    public static final String VAL_SHORTEST = "shortest";
    public static final String VAL_FASTEST = "fastest";

    public static String makeProfileName(String vehicleName, String weightingName, boolean hasTurnCosts) {
        String profileName = vehicleName + "_" + weightingName;
        if (hasTurnCosts)
            profileName += "_with_turn_costs";
        return profileName;
    }

    /**
     * 根据输入的权重为请求设置 weightingMethod。
     *
     * @param map             用于构建请求的 hints map
     * @param requestWeighting 最初请求的权重
     * @param profileType     对 HGV 而言是必需的
     */
    public static void setWeightingMethod(PMap map, int requestWeighting, int profileType, boolean hasTimeDependentSpeed) {
        //默认值
        String weightingMethod = VAL_RECOMMENDED;

        if (requestWeighting == WeightingMethod.SHORTEST)
            weightingMethod = VAL_SHORTEST;

        //对于请求推荐的权重，自行车、步行和 HGV 使用 recommended，汽车使用 fastest。
        if (requestWeighting == WeightingMethod.RECOMMENDED || requestWeighting == WeightingMethod.FASTEST) {
            if (profileType == RoutingProfileType.DRIVING_CAR) {
                weightingMethod = VAL_FASTEST;
            }
            if (RoutingProfileType.isHeavyVehicle(profileType) || RoutingProfileType.isCycling(profileType) || RoutingProfileType.isWalking(profileType)) {
                weightingMethod = VAL_RECOMMENDED;
            }
        }

        if (requestWeighting == WeightingMethod.CUSTOM)
            weightingMethod = VAL_CUSTOM;

        map.putObject(KEY_WEIGHTING_METHOD, weightingMethod);

        if (hasTimeDependentSpeed)
            map.putObject(GRParameters.Weighting.TIME_DEPENDENT_SPEED_OR_ACCESS, true);
    }

    /**
     * 根据输入的权重为请求设置权重。
     *
     * @param map             用于构建请求的 hints map
     * @param requestWeighting 最初请求的权重
     * @param profileType     对 HGV 而言是必需的
     */
    public static void setWeighting(PMap map, int requestWeighting, int profileType, boolean hasTimeDependentSpeed) {
        //默认值
        String weighting = VAL_RECOMMENDED;

        if (requestWeighting == WeightingMethod.SHORTEST)
            weighting = VAL_SHORTEST;

        //对于请求推荐的权重，自行车、步行和 HGV 使用 recommended，汽车使用 fastest。
        if (requestWeighting == WeightingMethod.RECOMMENDED || requestWeighting == WeightingMethod.FASTEST) {
            if (profileType == RoutingProfileType.DRIVING_CAR) {
                weighting = VAL_FASTEST;
            }
            if (RoutingProfileType.isHeavyVehicle(profileType) || RoutingProfileType.isCycling(profileType) || RoutingProfileType.isWalking(profileType)) {
                weighting = VAL_RECOMMENDED;
            }
        }

        map.putObject(KEY_WEIGHTING, weighting);

        if (hasTimeDependentSpeed)
            map.putObject(GRParameters.Weighting.TIME_DEPENDENT_SPEED_OR_ACCESS, true);
    }
}
