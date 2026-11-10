package org.heigit.geovisroute.coordinates_generator.cli;

import org.heigit.geovisroute.benchmark.exceptions.CommandLineParsingException;
import org.heigit.geovisroute.coordinates_generator.generators.CoordinateGeneratorSnapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SnappingCommandLineParserTest {

    @Test
    void testValidCliArguments() {
        String[] args = {
            "-n", "100",
                "-e", "8.6,49.3,8.7,49.4",
                    "-p", "driving-car,cycling-regular",
            "-r", "350",
            "-u", "http://localhost:8080/ors"
        };

        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        CoordinateGeneratorSnapping generator = cli.createGenerator();

        assertNotNull(generator);
        assertEquals("snapped_coordinates.csv", cli.getOutputFile());
        assertFalse(cli.hasHelp());
    }

    @Test
    void testCustomOutputFile() {
        String[] args = {
            "-n", "100",
                "-e", "8.6,49.3,8.7,49.4",
                    "-p", "driving-car",
            "-o", "custom_output.csv"
        };

        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        assertEquals("custom_output.csv", cli.getOutputFile());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "driving-car cycling-regular",
        "driving-car,cycling-regular",
        "driving-car, cycling-regular",
        "driving-car,cycling-regular,walking"
    })
    void testProfileParsing(String profileInput) {
        String[] args = {
            "-n", "100",
                "-e", "8.6,49.3,8.7,49.4",
                    "-p", profileInput
        };

        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        CoordinateGeneratorSnapping generator = cli.createGenerator();
        assertNotNull(generator);
    }

    @Test
    void testHelpFlag() {
        String[] args = { "-h" };
        // 断言抛出 MissingOptionException
        CommandLineParsingException exception = assertThrows(
                        CommandLineParsingException.class,
                () -> new SnappingCommandLineParser(args));
        assertNotNull(exception);
        assertEquals("org.heigit.geovisroute.benchmark.exceptions.CommandLineParsingException: Failed to parse command line arguments",
                exception.toString());
    }

    @Test
    void testMissingRequiredArgument() {
        String[] args = {
            "-n", "100",
                "-e", "8.6,49.3,8.7,49.4"
            // 缺少必需的 -p 参数
        };

        CommandLineParsingException exception = assertThrows(
                CommandLineParsingException.class, () -> new SnappingCommandLineParser(args));
        assertNotNull(exception);
    }

    @Test
    void testInvalidNumberFormat() {
        String[] args = {
            "-n", "invalid",
                "-e", "8.6,49.3,8.7,49.4",
                    "-p", "driving-car"
        };

        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        NumberFormatException exception = assertThrows(NumberFormatException.class, cli::createGenerator);
        assertNotNull(exception);
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
                "-n", "100",
                "-e", extentInput,
                "-p", "driving-car"
        };
        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        double[] extent = cli.parseExtent(extentInput);

        assertEquals(minLon, extent[0], 0.001, "Min longitude should match");
        assertEquals(minLat, extent[1], 0.001, "Min latitude should match");
        assertEquals(maxLon, extent[2], 0.001, "Max longitude should match");
        assertEquals(maxLat, extent[3], 0.001, "Max latitude should match");
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
                "-n", "100",
                "-e", extentInput,
                "-p", "driving-car"
        };
        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cli.parseExtent(extentInput),
                "Should throw exception for invalid extent format");
        assertNotNull(exception);
    }

    @Test
    void testFlexibleExtentCommandLine() {
        String[] args = {
                "-n", "100",
                "-e", "8.6,49.3,8.7,49.4", // 逗号分隔的范围
                "-p", "driving-car"
        };

        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        CoordinateGeneratorSnapping generator = cli.createGenerator();
        assertNotNull(generator);
    }

    @Test
    void testSpaceExtentCommandLine() {
        String[] args = {
            "-n", "100",
                "-e", "8.6 49.3 8.7 49.4", // 空格分隔的范围
            "-p", "driving-car"
        };

        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        CoordinateGeneratorSnapping generator = cli.createGenerator();
        assertNotNull(generator);
    }

    @Test
    void testEmptyProfileList() {
        String[] args = {
            "-n", "100",
                "-e", "8.6 49.3 8.7 49.4",
                    "-p", ""
        };
        SnappingCommandLineParser cli = new SnappingCommandLineParser(args);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                cli::createGenerator);
        assertNotNull(exception);
    }
}
