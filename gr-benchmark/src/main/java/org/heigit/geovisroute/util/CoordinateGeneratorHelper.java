package org.heigit.geovisroute.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于为基准测试生成坐标的辅助工具类。
 */
public class CoordinateGeneratorHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateGeneratorHelper.class);
    private static final double EARTH_RADIUS_METERS = 6371000; // 地球半径（米）

    private static final SecureRandom random = new SecureRandom();

    /**
     * 私有构造函数，防止实例化此工具类。
     */
    private CoordinateGeneratorHelper() {
        // 此构造函数不应被调用
    }

    /**
     * 生成一个随机坐标，既位于距中心点的半径范围内，又位于指定的范围之内。
     *
     * @param center       中心点 [lon, lat]
     * @param radiusMeters 距中心点的最大半径（米）
     * @param extent       边界框 [minLon, minLat, maxLon, maxLat]
     * @return 满足两个约束的随机坐标 [lon, lat]；若无法生成则返回 null
     */
    public static double[] randomCoordinateInRadiusAndExtent(double[] center, double radiusMeters, double[] extent) {
        if (center == null || center.length != 2) {
            throw new IllegalArgumentException("Center point must be a double array with 2 values [lon, lat]");
        }
        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("Radius must be greater than 0");
        }
        if (extent == null || extent.length != 4) {
            throw new IllegalArgumentException(
                    "Extent must be a double array with 4 values [minLon, minLat, maxLon, maxLat]");
        }

        // 首先检查中心点是否在范围内，否则无法生成
        if (!isWithinExtent(center, extent)) {
            LOGGER.debug("Center point is outside the specified extent - impossible to find a point");
            return new double[0];
        }

        // 替代方案：在半径内寻找一个同时位于范围内的点
        double[] candidate = findPointWithinRadiusAndExtent(center, radiusMeters, extent);
        if (candidate.length == 2) {
            return candidate;
        }

        LOGGER.error("Could not find a point that satisfies both constraints");
        // 若未找到点则返回空数组
        return new double[0];
    }

    private static double[] findPointWithinRadiusAndExtent(double[] center, double radiusMeters, double[] extent) {
        int maxAttempts = 100;
        for (int i = 0; i < maxAttempts; i++) {
            double[] candidate = randomCoordinateInRadius(center, radiusMeters);
            if (isWithinExtent(candidate, extent)) {
                LOGGER.debug("Generated point using alternative approach after {} attempt(s)", i + 1);
                return candidate;
            }
        }
        return new double[0];
    }

    /**
     * 在距中心点随机距离处生成一个随机坐标。
     *
     * @param center       中心点 [lon, lat]
     * @param radiusMeters 最大半径（米）
     * @return 距离在 1 到 radiusMeters 之间的随机坐标 [lon, lat]
     */
    public static double[] randomCoordinateInRadius(double[] center, double radiusMeters) {
        if (center == null || center.length != 2) {
            throw new IllegalArgumentException("Center point must be a double array with 2 values [lon, lat]");
        }
        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("Radius must be greater than 0");
        }

        // 生成一个随机角度（弧度，0 到 2π）
        double angle = random.nextDouble() * 2 * Math.PI;

        // 生成一个 1 到 radiusMeters 之间的随机半径
        double randomRadius = 1 + random.nextDouble() * (radiusMeters - 1);

        // 将随机距离处的极坐标转换为笛卡尔坐标
        return calculatePointAtDistanceAndBearing(center, randomRadius, angle);
    }

    /**
     * 计算距中心点指定距离和方位角处的坐标。
     *
     * @param center         中心点 [lon, lat]
     * @param distanceMeters 距离（米）
     * @param bearingRadians 方位角（弧度，0 = 北，π/2 = 东，等等）
     * @return 指定距离和方位角处的坐标 [lon, lat]
     */
    private static double[] calculatePointAtDistanceAndBearing(double[] center, double distanceMeters,
            double bearingRadians) {
        // 将中心点转换为弧度
        double lat1 = Math.toRadians(center[1]);
        double lon1 = Math.toRadians(center[0]);

        // 角距离（弧度）
        double angDist = distanceMeters / EARTH_RADIUS_METERS;

        // 计算新的纬度
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(angDist) +
                Math.cos(lat1) * Math.sin(angDist) * Math.cos(bearingRadians));

        // 计算新的经度
        double lon2 = lon1 + Math.atan2(
                Math.sin(bearingRadians) * Math.sin(angDist) * Math.cos(lat1),
                Math.cos(angDist) - Math.sin(lat1) * Math.sin(lat2));

        // 转换回度数
        double newLat = Math.toDegrees(lat2);
        double newLon = Math.toDegrees(lon2);

        // 将经度归一化到 -180 至 +180
        if (newLon > 180) {
            newLon -= 360;
        } else if (newLon < -180) {
            newLon += 360;
        }

        return new double[] { newLon, newLat };
    }

    /**
     * 在给定范围内生成随机坐标。
     *
     * @param count  要生成的坐标数量
     * @param extent 边界框 [minX, minY, maxX, maxY]
     * @return 坐标列表
     */
    public static List<double[]> randomCoordinatesInExtent(int count, double[] extent) {
        LOGGER.debug("Generating {} random coordinates within extent [{}, {}, {}, {}]",
                count, extent[0], extent[1], extent[2], extent[3]);

        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(generateRandomPoint(extent));
        }
        return points;
    }

    public static double[] generateRandomPoint(double[] extent) {
        int maxTries = 100;
        int tries = 0;
        double[] point;
        do {
            double x = random.nextDouble() * (extent[2] - extent[0]) + extent[0];
            double y = random.nextDouble() * (extent[3] - extent[1]) + extent[1];
            point = new double[] { x, y };
            tries++;
            if (tries >= maxTries) {
                LOGGER.warn("Failed to generate point within extent after {} tries", maxTries);
                return point; // 返回最后一次尝试的点
            }
        } while (!isWithinExtent(point, extent));
        return point;
    }

    private static boolean isWithinExtent(double[] point, double[] extent) {
        return point[0] >= extent[0] && point[0] <= extent[2] &&
                point[1] >= extent[1] && point[1] <= extent[3];
    }

    /**
     * 计算两点之间的 haversine 距离（米）。
     * 
     * @param p1 第一个点 [lon, lat]
     * @param p2 第二个点 [lon, lat]
     * @return 距离（米）
     */
    public static double calculateHaversineDistance(double[] p1, double[] p2) {
        double lat1 = Math.toRadians(p1[1]);
        double lon1 = Math.toRadians(p1[0]);
        double lat2 = Math.toRadians(p2[1]);
        double lon2 = Math.toRadians(p2[0]);

        double latDistance = lat2 - lat1;
        double lonDistance = lon2 - lon1;

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c; // 距离（米）
    }

}
