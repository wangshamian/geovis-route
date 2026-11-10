package org.heigit.geovisroute.coordinates_generator.cli;

import org.heigit.geovisroute.benchmark.exceptions.CommandLineParsingException;
import org.heigit.geovisroute.coordinates_generator.generators.CoordinateGeneratorMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MatrixCommandLineParserTest {

        @Test
        void testValidCliArguments() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car,cycling-regular",
                                "-u", "http://localhost:8080/ors",
                                "-m", "driving-car:5000,cycling-regular:3000",
                                "-r", "3",
                                "-c", "3",
                                "-t", "4"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                CoordinateGeneratorMatrix generator = cli.createGenerator();

                assertNotNull(generator);
                assertEquals("matrices.csv", cli.getOutputFile());
                assertFalse(cli.hasHelp());
        }

        @Test
        void testCustomOutputFile() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car",
                                "-m", "driving-car:5000",
                                "-o", "custom_matrices.csv"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                assertEquals("custom_matrices.csv", cli.getOutputFile());
        }

        @ParameterizedTest
        @CsvSource({
                        "driving-car,driving-car:5000",
                        "'driving-car,cycling-regular','driving-car:5000,cycling-regular:3000'",
                        "'driving-car,cycling-regular,foot-walking','driving-car:5000,cycling-regular:3000,foot-walking:1000'"
        })
        void testProfileParsing(String profileInput, String maxDistancesInput) {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-m", maxDistancesInput,
                                "-p", profileInput
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                CoordinateGeneratorMatrix generator = cli.createGenerator();
                assertNotNull(generator);
        }

        @Test
        void testHelpFlag() {
                String[] args = { "-h" };
                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                assertTrue(cli.hasHelp());
        }

        @Test
        void testMissingRequiredArgument() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4"
                                // 缺少必需的 -p 参数
                };

                CommandLineParsingException exception = assertThrows(
                                CommandLineParsingException.class, () -> new MatrixCommandLineParser(args));
                assertNotNull(exception);
        }

        @Test
        void testInvalidNumberFormat() {
                String[] args = {
                                "-n", "invalid",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car",
                                "-m", "driving-car:5000"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                NumberFormatException exception = assertThrows(NumberFormatException.class, cli::createGenerator);
                assertNotNull(exception);
        }

        @Test
        void testInvalidExtentValues() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, ", // 缺少一个范围值
                                "-p", "driving-car",
                                "-m", "driving-car:5000"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class, cli::createGenerator);
                assertNotNull(exception);
        }

        @Test
        void testEmptyProfileList() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", ""
                };
                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                cli::createGenerator);
                assertNotNull(exception);
        }

        @ParameterizedTest
        @ValueSource(strings = { "2", "3", "4", "5" })
        void testMatrixDimensionsParsing(String dimension) {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car",
                                "-m", "driving-car:5000",
                                "-r", dimension,
                                "-c", dimension
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                CoordinateGeneratorMatrix generator = cli.createGenerator();
                assertNotNull(generator);

                // 使用反射测试行和列维度
                assertDoesNotThrow(() -> {
                        Field rowsField = CoordinateGeneratorMatrix.class.getDeclaredField("numRows");
                        rowsField.setAccessible(true);
                        assertEquals(Integer.parseInt(dimension), rowsField.getInt(generator));

                        Field colsField = CoordinateGeneratorMatrix.class.getDeclaredField("numCols");
                        colsField.setAccessible(true);
                        assertEquals(Integer.parseInt(dimension), colsField.getInt(generator));
                });
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource({
                        "driving-car:5000,cycling-regular:3000",
                        "driving-car:10000,cycling-regular:5000",
                        "driving-car:20000,cycling-regular:10000"
        })
        void testMaxDistancesParsing(String maxDistances) {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car,cycling-regular",
                                "-m", maxDistances
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                CoordinateGeneratorMatrix generator = cli.createGenerator();

                // 使用反射访问 maxDistanceByProfile 字段
                assertDoesNotThrow(() -> {
                        Field maxDistanceByProfileField = CoordinateGeneratorMatrix.class
                                        .getDeclaredField("maxDistanceByProfile");
                        maxDistanceByProfileField.setAccessible(true);
                        Map<String, Double> maxDistanceByProfile = (Map<String, Double>) maxDistanceByProfileField
                                        .get(generator);
                        assertNotNull(maxDistanceByProfile);

                        String[] entries = maxDistances.split(",");
                        for (String entry : entries) {
                                String[] parts = entry.split(":");
                                assertEquals(Double.valueOf(parts[1]), maxDistanceByProfile.get(parts[0]));
                        }
                });
        }

        @ParameterizedTest
        @ValueSource(strings = {
                        "driving-car:5000,cycling-regular:3000,foot-walking:1000", // 距离数量多于配置方案数量
                        "driving-car:5000" // 距离数量少于配置方案数量（在此实现中会通过，因为使用了默认值）
        })
        void testMismatchedMaxDistancesAndProfiles(String maxDistances) {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car,cycling-regular", // 2 个配置方案
                                "-m", maxDistances
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);

                // 与 RouteCommandLineParser 不同，maxDistances 无需与配置方案完全匹配
                // 因为实现是按配置方案名称映射的
                CoordinateGeneratorMatrix generator = cli.createGenerator();
                assertNotNull(generator);
        }

        @Test
        void testInvalidMaxDistance() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car",
                                "-m", "driving-car:invalid"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                NumberFormatException exception = assertThrows(NumberFormatException.class,
                                cli::createGenerator);
                assertNotNull(exception);
        }

        @Test
        void testThreadCount() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car",
                                "-m", "driving-car:5000",
                                "-t", "8"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                CoordinateGeneratorMatrix generator = cli.createGenerator();
                assertNotNull(generator);

                // 使用反射测试线程数
                assertDoesNotThrow(() -> {
                        Field threadCountField = CoordinateGeneratorMatrix.class.getDeclaredField("numThreads");
                        threadCountField.setAccessible(true);
                        assertEquals(8, threadCountField.getInt(generator));
                });
        }

        @Test
        void testInvalidThreadCount() {
                String[] args = {
                                "-n", "50",
                                "-e", "8.6, 49.3, 8.7, 49.4",
                                "-p", "driving-car",
                                "-m", "driving-car:5000",
                                "-t", "invalid"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                NumberFormatException exception = assertThrows(NumberFormatException.class,
                                cli::createGenerator);
                assertNotNull(exception);
        }

        @ParameterizedTest
        @CsvSource({
                        // 格式：范围字符串，期望值
                        "'8.6, 49.3, 8.7, 49.4', 8.6, 49.3, 8.7, 49.4",
                        "'8.6,49.3,8.7,49.4', 8.6, 49.3, 8.7, 49.4",
        })
        void testExtentParsing(String extentInput, double minLon, double minLat, double maxLon, double maxLat) {
                // 不使用反射测试私有的 parseExtent 方法，
                // 而是通过公有的 createGenerator 方法测试其行为
                String[] args = {
                                "-n", "50",
                                "-e", extentInput,
                                "-p", "driving-car",
                                "-m", "driving-car:5000"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);
                CoordinateGeneratorMatrix generator = cli.createGenerator();

                // 验证生成器已使用正确的范围值成功创建，
                // 并通过反射检查生成器的内部状态
                assertDoesNotThrow(() -> {
                        Field extentField = CoordinateGeneratorMatrix.class.getSuperclass().getDeclaredField("extent");
                        extentField.setAccessible(true);
                        double[] extent = (double[]) extentField.get(generator);

                        assertArrayEquals(
                                        new double[] { minLon, minLat, maxLon, maxLat },
                                        extent,
                                        0.001,
                                        "Extent should be parsed correctly regardless of format");
                });
        }

        @ParameterizedTest
        @ValueSource(strings = {
                        "8.6,49.3,8.7", // 值太少
                        "8.6,invalid,8.7,49.4", // 非数字值
        })
        void testInvalidExtentParsing(String extentInput) {
                // 通过公有的 createGenerator 方法进行测试
                String[] args = {
                                "-n", "50",
                                "-e", extentInput,
                                "-p", "driving-car",
                                "-m", "driving-car:5000"
                };

                MatrixCommandLineParser cli = new MatrixCommandLineParser(args);

                // 当给定无效范围时，createGenerator 方法应抛出 IllegalArgumentException
                Exception exception = assertThrows(
                                Exception.class,
                                cli::createGenerator,
                                "Should throw exception for invalid extent format");

                // 它可能直接是 IllegalArgumentException，也可能嵌套在另一种异常中
                assertTrue(exception instanceof IllegalArgumentException ||
                                (exception.getCause() != null
                                                && exception.getCause() instanceof IllegalArgumentException));
        }
}