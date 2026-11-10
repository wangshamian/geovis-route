package org.heigit.geovisroute.coordinates_generator.cli;

import org.apache.commons.cli.*;
import org.heigit.geovisroute.benchmark.exceptions.CommandLineParsingException;
import org.heigit.geovisroute.coordinates_generator.generators.CoordinateGeneratorSnapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnappingCommandLineParser extends CommandLineParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnappingCommandLineParser.class);

    public SnappingCommandLineParser(String[] args) {
        super(args);
    }

    @Override
    protected void setupOptions() {
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build());

        options.addOption(Option.builder("n")
                .longOpt("num-points")
                .hasArg()
                .required()
                .type(Number.class)
                .desc("Number of points to generate")
                .build());

        options.addOption(Option.builder("e")
                .longOpt("extent")
                .hasArg()
                .required()
                .desc("Bounding box (minLon minLat maxLon maxLat)")
                .build());

        options.addOption(Option.builder("r")
                .longOpt("radius")
                .hasArg()
                .type(Number.class)
                .desc("Search radius in meters (default: 350)")
                .build());

        options.addOption(Option.builder("p")
                .longOpt("profiles")
                .hasArg()
                .required()
                .desc("Comma-separated routing profiles (e.g., driving-car,cycling-regular)")
                .build());

        options.addOption(Option.builder("u")
                .longOpt("url")
                .hasArg()
                .desc("ORS API base URL")
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .desc("Output CSV file path")
                .build());
    }

    @Override
    protected CommandLine parseCommandLine(String[] args) {
        try {
            return new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            printHelp();
            throw new CommandLineParsingException("Failed to parse command line arguments", e);
        }
    }

    @Override
    public void printHelp() {
        new HelpFormatter().printHelp("CoordinateGeneratorSnapping", options, true);
    }

    @Override
    protected String getDefaultOutputFile() {
        return "snapped_coordinates.csv";
    }

    public CoordinateGeneratorSnapping createGenerator() {
        int numPoints = Integer.parseInt(cmd.getOptionValue("n"));
        double[] extent = parseExtent(cmd.getOptionValue("e"));
        String[] profiles = parseProfiles(cmd.getOptionValue("p"));
        double radius = Double.parseDouble(cmd.getOptionValue("r", "350"));
        String baseUrl = cmd.getOptionValue("u", "http://localhost:8080/ors");

        LOGGER.info(
                "Creating CoordinateGeneratorSnapping with numPoints={}, extent={}, radius={}, profiles={}, baseUrl={}",
                numPoints, extent, radius, profiles, baseUrl);

        return new CoordinateGeneratorSnapping(numPoints, extent, radius, profiles, baseUrl);
    }

    /**
     * 将范围字符串解析为包含四个 double 的数组。
     * 支持逗号分隔、空格分隔或混合格式。
     * 
     * @param extentStr 要解析的范围字符串
     * @return 表示最小经度、最小纬度、最大经度、最大纬度的四个 double 数组
     * @throws IllegalArgumentException 如果范围无法正确解析
     */
    public double[] parseExtent(String extentStr) {
        if (extentStr == null || extentStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Extent string cannot be empty");
        }

        // 将逗号替换为空格并按空白拆分
        String normalized = extentStr.replace(',', ' ').trim();
        String[] parts = normalized.split("\\s+");

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Extent must contain exactly 4 values (minLon, minLat, maxLon, maxLat), but found " + parts.length);
        }

        double[] extent = new double[4];
        try {
            for (int i = 0; i < 4; i++) {
                extent[i] = Double.parseDouble(parts[i]);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numerical value in extent: " + e.getMessage());
        }

        return extent;
    }
}
