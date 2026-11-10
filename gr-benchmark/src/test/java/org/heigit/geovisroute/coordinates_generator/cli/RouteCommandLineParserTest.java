package org.heigit.geovisroute.coordinates_generator.cli;

import org.heigit.geovisroute.benchmark.exceptions.CommandLineParsingException;
import org.heigit.geovisroute.coordinates_generator.generators.CoordinateGeneratorRoute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteCommandLineParserTest {

    @Test
    void testValidCliArguments() {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car,cycling-regular",
                "-u", "http://localhost:8080/ors",
                "-d", "100",
                "-m", "5000,3000",
                "-t", "4"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();

        assertNotNull(generator);
        assertEquals("route_coordinates.csv", cli.getOutputFile());
        assertFalse(cli.hasHelp());
    }

    @Test
    void testCustomOutputFile() {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car",
                "-o", "custom_routes.csv"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        assertEquals("custom_routes.csv", cli.getOutputFile());
    }

    @ParameterizedTest
    @CsvSource({
            "driving-car,5000",
            "'driving-car,cycling-regular','5000,3000'",
            "'driving-car,cycling-regular,foot-walking','5000,3000,1000'"
    })
    void testProfileParsing(String profileInput, String maxDistances) {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-m", maxDistances,
                "-p", profileInput
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();
        assertNotNull(generator);
    }

    @Test
    void testHelpFlag() {
        String[] args = { "-h" };
        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        assertTrue(cli.hasHelp());

    }

    @Test
    void testMissingRequiredArgument() {
        String[] args = {
                "-n", "50",
                "-e", "8.6", "49.3", "8.7", "49.4"
                // 缺少必需的 -p 参数
        };

        CommandLineParsingException exception = assertThrows(
                CommandLineParsingException.class, () -> new RouteCommandLineParser(args));
        assertNotNull(exception);
    }

    @Test
    void testInvalidNumberFormat() {
        String[] args = {
                "-n", "invalid",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        NumberFormatException exception = assertThrows(NumberFormatException.class, cli::createGenerator);
        assertNotNull(exception);
    }

    @Test
    void testInvalidExtentValues() {
        String[] args = {
                "-n", "50",
                "-e", "8.6", "49.3", "8.7", // 缺少一个范围值
                "-p", "driving-car"
        };

        RouteCommandLineParser routeCommandLineParser = new RouteCommandLineParser(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, routeCommandLineParser::createGenerator);
        assertNotNull(exception);
    }

    @Test
    void testEmptyProfileList() {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", ""
        };
        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                cli::createGenerator);
        assertNotNull(exception);
    }

    @ParameterizedTest
    @ValueSource(strings = { "100", "250", "500", "1000" })
    void testMinDistanceParsing(String minDistance) {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                        "-p", "driving-car",
                "-d", minDistance,
                "-m", "5000"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();
        assertNotNull(generator);
        // 使用反射测试 minDistance
        assertDoesNotThrow(() -> {
            Field minDistanceField = CoordinateGeneratorRoute.class.getDeclaredField("minDistance");
            minDistanceField.setAccessible(true);
            assertEquals(Double.valueOf(minDistance), minDistanceField.get(generator));
        });
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(strings = {
            "5000,3000",
            "10000,5000",
            "20000,10000"
    })
    void testMaxDistancesParsing(String maxDistances) {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car,cycling-regular",
                "-m", maxDistances
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();

        // 使用反射访问 maxDistanceByProfile 字段
        assertDoesNotThrow(() -> {
            Field maxDistanceByProfileField = CoordinateGeneratorRoute.class.getDeclaredField("maxDistanceByProfile");
            maxDistanceByProfileField.setAccessible(true);
            Map<String, Double> maxDistanceByProfile = (Map<String, Double>) maxDistanceByProfileField.get(generator);
            assertNotNull(maxDistanceByProfile);
            String[] maxDistancesArray = maxDistances.split(",");
            assertEquals(Double.valueOf(maxDistancesArray[0]), maxDistanceByProfile.get("driving-car"));
            assertEquals(Double.valueOf(maxDistancesArray[1]), maxDistanceByProfile.get("cycling-regular"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5000,3000,1000", // 距离数量多于配置方案数量
            "5000" // 距离数量少于配置方案数量
    })
    void testMismatchedMaxDistancesAndProfiles(String maxDistances) {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car,cycling-regular", // 2 个配置方案
                "-m", maxDistances
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, cli::createGenerator);
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Number of max distances"),
                "Exception message should mention number of max distances");
        assertTrue(exception.getMessage().contains("must match number of profiles"),
                "Exception message should mention matching profiles");
    }

    @SuppressWarnings("java:S5976")
    @Test
    void testInvalidMaxDistance() {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car",
                "-m", "invalid"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();
        assertNotNull(generator); // 仍应创建生成器，但会记录警告
    }

    @Test
    void testThreadCount() {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car",
                "-m", "5000",
                "-t", "8"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();
        assertNotNull(generator);
    }

    @Test
    void testInvalidThreadCount() {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4",
                "-p", "driving-car",
                "-m", "1000",
                "-t", "invalid"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                cli::createGenerator);
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Invalid number of threads"));
    }

    @ParameterizedTest
    @CsvSource({
            // 格式：范围字符串，期望值
            "'8.6,49.3,8.7,49.4', 8.6, 49.3, 8.7, 49.4",
            "'8.6 49.3 8.7 49.4', 8.6, 49.3, 8.7, 49.4",
            "'8.6, 49.3, 8.7, 49.4', 8.6, 49.3, 8.7, 49.4"
    })
    void testExtentParsing(String extentInput, double minLon, double minLat, double maxLon, double maxLat) {
        String[] args = {
                "-n", "50",
                "-e", extentInput,
                "-p", "driving-car",
                "-m", "1000"
        };
        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        double[] extent = cli.parseExtent(extentInput);

        assertArrayEquals(
                new double[] { minLon, minLat, maxLon, maxLat },
                extent,
                0.001,
                "Extent should be parsed correctly regardless of format");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "8.6,49.3,8.7", // 值太少
            "8.6,49.3,8.7,49.4,8.8", // 值太多
            "8.6,invalid,8.7,49.4", // 非数字值
            "", // 空字符串
            "   " // 空白字符串
    })
    void testInvalidExtentParsing(String extentInput) {
        String[] args = {
                "-n", "50",
                "-e", extentInput,
                "-p", "driving-car",
                "-m", "1000"
        };
        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cli.parseExtent(extentInput),
                "Should throw exception for invalid extent format");
        assertNotNull(exception);
    }

    @Test
    void testFlexibleExtentCommandLine() {
        String[] args = {
                "-n", "50",
                "-e", "8.6,49.3,8.7,49.4", // 逗号分隔的范围
                "-p", "driving-car",
                "-m", "5000"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();
        assertNotNull(generator);
    }

    @Test
    void testSpaceExtentCommandLine() {
        String[] args = {
                "-n", "50",
                "-e", "8.6 49.3 8.7 49.4", // 空格分隔的范围
                "-p", "driving-car",
                "-m", "5000"
        };

        RouteCommandLineParser cli = new RouteCommandLineParser(args);
        CoordinateGeneratorRoute generator = cli.createGenerator();
        assertNotNull(generator);
    }
}
