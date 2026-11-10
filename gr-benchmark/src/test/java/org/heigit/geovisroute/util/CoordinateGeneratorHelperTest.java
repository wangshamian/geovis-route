package org.heigit.geovisroute.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CoordinateGeneratorHelper 工具类的测试。
 */
class CoordinateGeneratorHelperTest {

    private static final double[] TEST_EXTENT = new double[] { 8.573179, 49.352003, 8.793986, 49.459693 };
    private static final double DELTA = 1e-10;

    @Test
    void randomCoordinatesInExtent_WithoutConstraints_ReturnsRequestedNumberOfCoordinates() {
        // 准备
        int count = 10;

        // 执行
        List<double[]> coordinates = CoordinateGeneratorHelper.randomCoordinatesInExtent(count, TEST_EXTENT);

        // 验证
        assertEquals(count, coordinates.size());
        for (double[] coord : coordinates) {
            assertCoordinateWithinExtent(coord, TEST_EXTENT);
        }
    }

    @Test
    void calculateHaversineDistance_ReturnsCorrectDistance() {
        // 准备
        double[] berlin = new double[] { 13.405, 52.52 }; // 柏林
        double[] paris = new double[] { 2.3522, 48.8566 }; // 巴黎
        double expectedDistance = 878600; // 约 878.6 km（直线距离）
        double tolerance = 1200; // 允许 1.2 km 容差

        // 执行
        double distance = CoordinateGeneratorHelper.calculateHaversineDistance(berlin, paris);

        // 验证
        assertEquals(expectedDistance, distance, tolerance,
                "Distance between Berlin and Paris should be approximately 878.6 km");
    }
    
    @Test
    void calculateHaversineDistance_SamePoint_ReturnsZero() {
        // 准备
        double[] point = new double[] { 8.676, 49.418 };
        
        // 执行
        double distance = CoordinateGeneratorHelper.calculateHaversineDistance(point, point);
        
        // 验证
        assertEquals(0.0, distance, DELTA, "Distance between same points should be zero");
    }

    /**
     * 测试生成的多个点分布在整个范围内。
     */
    @Test
    void randomCoordinatesInExtent_MultiplePoints_AreDistributed() {
        int numPoints = 100;
        List<double[]> points = CoordinateGeneratorHelper.randomCoordinatesInExtent(numPoints, TEST_EXTENT);

        // 通过将范围划分为四个象限并验证点落入各象限来检查分布情况
        double midLon = (TEST_EXTENT[0] + TEST_EXTENT[2]) / 2;
        double midLat = (TEST_EXTENT[1] + TEST_EXTENT[3]) / 2;

        boolean foundNW = false;
        boolean foundNE = false;
        boolean foundSW = false;
        boolean foundSE = false;

        for (double[] point : points) {
            if (point[0] < midLon && point[1] > midLat)
                foundNW = true;
            if (point[0] >= midLon && point[1] > midLat)
                foundNE = true;
            if (point[0] < midLon && point[1] <= midLat)
                foundSW = true;
            if (point[0] >= midLon && point[1] <= midLat)
                foundSE = true;

            // 若所有象限都已命中，则可以停止检查
            if (foundNW && foundNE && foundSW && foundSE)
                break;
        }

        assertTrue(foundNW, "Should have at least one point in the northwest quadrant");
        assertTrue(foundNE, "Should have at least one point in the northeast quadrant");
        assertTrue(foundSW, "Should have at least one point in the southwest quadrant");
        assertTrue(foundSE, "Should have at least one point in the southeast quadrant");
    }

    @Test
    void testRandomCoordinateInRadius_ValidatesNullCenter() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CoordinateGeneratorHelper.randomCoordinateInRadius(null, 1000);
        });
        assertEquals("Center point must be a double array with 2 values [lon, lat]", exception.getMessage());
    }

    @Test
    void testRandomCoordinateInRadius_ValidatesInvalidCenterSize() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CoordinateGeneratorHelper.randomCoordinateInRadius(new double[] { 1.0 }, 1000);
        });
        assertEquals("Center point must be a double array with 2 values [lon, lat]", exception.getMessage());
    }

    @Test
    void testRandomCoordinateInRadius_ValidatesNegativeRadius() {
        double[] center = { 13.4, 52.5 }; // 柏林
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CoordinateGeneratorHelper.randomCoordinateInRadius(center, -100);
        });
        assertEquals("Radius must be greater than 0", exception.getMessage());
    }

    @Test
    void testRandomCoordinateInRadius_ValidatesZeroRadius() {
        double[] center = { 8.6881, 49.4054 }; // 海德堡
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CoordinateGeneratorHelper.randomCoordinateInRadius(center, 0);
        });
        assertEquals("Radius must be greater than 0", exception.getMessage());
    }

    @RepeatedTest(100) // 多次运行此测试以验证随机性
    void testRandomCoordinateInRadius_GeneratesPointAtCorrectDistance() {
        double[] center = { 8.6881, 49.4054 }; // 海德堡
        double radius = 10000; // 10 km

        double[] result = CoordinateGeneratorHelper.randomCoordinateInRadius(center, radius);

        // 验证结果格式
        assertEquals(2, result.length);

        // 计算中心点与生成点之间的距离
        double distance = CoordinateGeneratorHelper.calculateHaversineDistance(center, result);

        assertTrue(distance <= radius, "Distance should be less than or equal to the radius");
    }

    @ParameterizedTest
    @ValueSource(doubles = { 2.0, 100.0, 1000.0, 10000.0, 50000.0 })
    void testRandomCoordinateInRadius_WorksWithDifferentRadii(double radius) {
        double[] center = { 0.0, 0.0 }; // 零点岛（Null Island）

        double[] result = CoordinateGeneratorHelper.randomCoordinateInRadius(center, radius);

        // 计算中心点与生成点之间的距离
        double distance = CoordinateGeneratorHelper.calculateHaversineDistance(center, result);

        // 距离应非常接近指定半径
        assertTrue(distance <= radius, "Distance should be less than or equal to the radius");
    }

    @RepeatedTest(10)
    void testRandomCoordinateInRadius_HandlesEdgeCases() {
        // 测试接近极点的情况
        double[] northPole = { 0.0, 89.9 }; // 靠近北极
        double[] result = CoordinateGeneratorHelper.randomCoordinateInRadius(northPole, 10000);
        double distance = CoordinateGeneratorHelper.calculateHaversineDistance(northPole, result);
        assertTrue(distance <= 10000, "Distance should be less than or equal to the radius");

        // 测试接近反子午线（180°/-180° 经度）的情况
        double[] antiMeridian = { 179.9, 0.0 };
        result = CoordinateGeneratorHelper.randomCoordinateInRadius(antiMeridian, 10000);
        distance = CoordinateGeneratorHelper.calculateHaversineDistance(antiMeridian, result);
        assertTrue(distance <= 10000, "Distance should be less than or equal to the radius");
    }

    @RepeatedTest(10)
     void testRandomCoordinateInRadius_GeneratesDifferentPoints() {
        double[] center = { 8.6881, 49.4054 }; // 海德堡
        double radius = 5000; // 5 km

        // 生成两个点并验证它们不同
        double[] point1 = CoordinateGeneratorHelper.randomCoordinateInRadius(center, radius);
        double[] point2 = CoordinateGeneratorHelper.randomCoordinateInRadius(center, radius);

        // 点应不同（它们完全相同的概率极低）
        boolean pointsAreDifferent = point1[0] != point2[0] || point1[1] != point2[1];
        assertTrue(pointsAreDifferent, "Generated points should be different");

        // 点之间的距离应在半径范围内
        double distance = CoordinateGeneratorHelper.calculateHaversineDistance(point1, point2);
        assertTrue(distance <= radius * 2, "Distance between points should be within the radius");
    }

    @Test
    void testRandomCoordinateInRadius_OutputFormat() {
        double[] center = { 8.6881, 49.4054 }; // 海德堡
        double radius = 1000; // 1 km

        double[] result = CoordinateGeneratorHelper.randomCoordinateInRadius(center, radius);

        // 验证得到的顺序是经度、纬度
        // 对于相对较小的半径，结果应仍然接近中心点
        assertTrue(Math.abs(result[0] - center[0]) < 1.0, "Longitude should be within 1 degree");
        assertTrue(Math.abs(result[1] - center[1]) < 1.0, "Latitude should be within 1 degree");
    }
    private void assertCoordinateWithinExtent(double[] coordinate, double[] extent, String message) {
        assertTrue(coordinate[0] >= extent[0] && coordinate[0] <= extent[2],
                message != null ? message : "Longitude " + coordinate[0] + " should be within [" + extent[0] + ", " + extent[2] + "]");
        assertTrue(coordinate[1] >= extent[1] && coordinate[1] <= extent[3],
                message != null ? message : "Latitude " + coordinate[1] + " should be within [" + extent[1] + ", " + extent[3] + "]");
    }

    private void assertCoordinateWithinExtent(double[] coordinate, double[] extent) {
        assertCoordinateWithinExtent(coordinate, extent, null);
    }

    @RepeatedTest(5)
    void testRandomCoordinateInRadiusAndExtent_MultipleCalls() {
        double[] center = {8.6821, 49.4141};
        double radiusMeters = 2000;
        double[] extent = {8.65, 49.39, 8.71, 49.44}; // 海德堡周边的限定区域
        
        // 生成多个点
        double[] point1 = CoordinateGeneratorHelper.randomCoordinateInRadiusAndExtent(center, radiusMeters, extent);
        double[] point2 = CoordinateGeneratorHelper.randomCoordinateInRadiusAndExtent(center, radiusMeters, extent);
        
        // 验证两个点都有效
        assertNotNull(point1);
        assertNotNull(point2);
        assertEquals(2, point1.length);
        assertEquals(2, point2.length);
        
        // 检查两个点都在半径范围内
        double distance1 = CoordinateGeneratorHelper.calculateHaversineDistance(center, point1);
        double distance2 = CoordinateGeneratorHelper.calculateHaversineDistance(center, point2);
        assertTrue(distance1 <= radiusMeters, "First point should be within the specified radius");
        assertTrue(distance2 <= radiusMeters, "Second point should be within the specified radius");
        
        // 检查两个点都在范围内
        assertCoordinateWithinExtent(point1, extent, "First point should be within extent");
        assertCoordinateWithinExtent(point2, extent, "Second point should be within extent");
        
        // 点应不同（它们完全相同的概率极低）
        boolean pointsAreDifferent = point1[0] != point2[0] || point1[1] != point2[1];
        assertTrue(pointsAreDifferent, "Generated points should be different");
    }
}