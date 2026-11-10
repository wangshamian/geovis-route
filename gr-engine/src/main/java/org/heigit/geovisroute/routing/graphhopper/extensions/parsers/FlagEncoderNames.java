package org.heigit.geovisroute.routing.graphhopper.extensions.parsers;

public class FlagEncoderNames {
    private static final String ORS_SUFFIX = "_ors";

    public static final String UNKNOWN = "UNKNOWN";

    public static final String CAR_ORS = "car" + ORS_SUFFIX;
    public static final String HEAVYVEHICLE = "heavyvehicle";
    public static final String EMERGENCY = "emergency";
    public static final String EVEHICLE = "evehicle";       // 未实现
    public static final String RUNNING = "running";        // 未实现

    public static final String WHEELCHAIR = "wheelchair";

    public static final String BIKE_ORS = "bike" + ORS_SUFFIX;
    public static final String ROADBIKE_ORS = "roadbike" + ORS_SUFFIX;
    public static final String MTB_ORS = "mtb" + ORS_SUFFIX;
    public static final String BIKE_ELECTRO = "electrobike";

    public static final String PEDESTRIAN_ORS = "pedestrian" + ORS_SUFFIX;
    public static final String HIKING_ORS = "hiking" + ORS_SUFFIX;


    public static final String GH_CAR = "car";
    public static final String GH_CAR4WD = "car4wd";
    public static final String GH_MOTOCYCLE = "motorcycle";
    public static final String GH_FOOT = "foot";
    public static final String GH_HIKE = "hike";
    public static final String GH_RACINGBIKE = "racingbike";
    public static final String GH_MTB = "mtb";
    public static final String GH_BIKE = "bike";
    public static final String GH_BIKE2 = "bike2";

    private FlagEncoderNames() {
    }

    public static String getBaseName(String name) {
        if (name.endsWith(ORS_SUFFIX))
            name = name.substring(0, name.indexOf(ORS_SUFFIX));

        return name;
    }

    /**
     * 将 ORS 的 encoder/车辆名称映射为 GraphHopper 11 编码值使用的车辆名称，例如
     * {@code car_ors -> car}。GraphHopper 11 只识别车辆类型 car、bike、racingbike、mtb 和 foot。
     */
    public static String getGHVehicleName(String name) {
        return switch (getBaseName(name)) {
            case GH_CAR, GH_CAR4WD, "emergency", "evehicle", "heavyvehicle" -> GH_CAR;
            case GH_BIKE, GH_BIKE2, "electrobike" -> GH_BIKE;
            case GH_RACINGBIKE, "roadbike" -> GH_RACINGBIKE;
            case GH_MTB -> GH_MTB;
            case GH_FOOT, GH_HIKE, "pedestrian", "hiking" -> GH_FOOT;
            default -> name;
        };
    }
}
