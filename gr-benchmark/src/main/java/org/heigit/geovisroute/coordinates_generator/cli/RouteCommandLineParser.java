package org.heigit.geovisroute.coordinates_generator.cli;

import org.apache.commons.cli.*;
import org.heigit.geovisroute.benchmark.exceptions.CommandLineParsingException;
import org.heigit.geovisroute.coordinates_generator.generators.CoordinateGeneratorRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RouteCommandLineParser extends CommandLineParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteCommandLineParser.class);

    private static final String OPT_THREADS = "threads";
    private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    public RouteCommandLineParser(String[] args) {
        super(args);
    }

    @Override
    protected void setupOptions() {
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build());

        options.addOption(Option.builder("n")
                .longOpt("num-routes")
                .hasArg()
                .required()
                .type(Number.class)
                .desc("Number of routes to generate")
                .build());

        options.addOption(Option.builder("e")
                .longOpt("extent")
                .hasArg()
                .required()
                .desc("Bounding box (minLon,minLat,maxLon,maxLat) or (minLon minLat maxLon maxLat)")
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

        options.addOption(Option.builder("d")
                .longOpt("min-distance")
                .hasArg()
                .type(Number.class)
                .desc("Minimum distance between coordinates in meters (default: 0)")
                .build());

        options.addOption(Option.builder("m")
                .longOpt("max-distances")
                .hasArg()
                .desc("Maximum distances between coordinates in meters, comma-separated in profile order (e.g., 5000,3000)")
                .build());

        options.addOption(Option.builder("t")
                .longOpt(OPT_THREADS)
                .hasArg()
                .desc("Number of threads to use (default: " + DEFAULT_THREAD_COUNT + ")")
                .build());
    }

    @Override
    protected CommandLine parseCommandLine(String[] args) {
        try {
            return new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            // 如果参数中包含 help，则打印帮助信息
            if (args != null && args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
                printHelp();
            } else {
                printHelp();
                throw new CommandLineParsingException("Failed to parse command line arguments", e);
            }
            return null;
        }
    }

    @Override
    public void printHelp() {
        new HelpFormatter().printHelp("CoordinateGeneratorRoute", options, true);
    }

    @Override
    protected String getDefaultOutputFile() {
        return "route_coordinates.csv";
    }

    public CoordinateGeneratorRoute createGenerator() {
        int numRoutes = Integer.parseInt(cmd.getOptionValue("n"));

        // 使用新的范围解析器
        double[] extent = parseExtent(cmd.getOptionValue("e"));

        String[] profiles = parseProfiles(cmd.getOptionValue("p"));
        String baseUrl = cmd.getOptionValue("u", "http://localhost:8080/ors");
        double minDistance = Double.parseDouble(cmd.getOptionValue("d", "1"));

        // 若提供则解析最大距离
        Map<String, Double> maxDistanceByProfile = parseMaxDistances(cmd.getOptionValue("m"), profiles);

        int numThreads = parseNumThreads(cmd.getOptionValue(OPT_THREADS, String.valueOf(DEFAULT_THREAD_COUNT)));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Creating CoordinateGeneratorRoute with numRoutes={}, extent={}, profiles={}, baseUrl={}, minDistance={}, maxDistances={}, numThreads={}",
                    numRoutes, extent, java.util.Arrays.toString(profiles), baseUrl, minDistance, maxDistanceByProfile,
                    numThreads);
        }

        return new CoordinateGeneratorRoute(numRoutes, extent, profiles, baseUrl, minDistance, maxDistanceByProfile,
                numThreads);
    }

    /**
     * 从字符串输入解析范围，支持逗号分隔和空格分隔两种格式
     * 
     * @param extentStr 包含范围坐标的字符串
     * @return 表示范围 [minLon, minLat, maxLon, maxLat] 的 4 个 double 数组
     * @throws IllegalArgumentException 如果范围无法正确解析
     */
    protected double[] parseExtent(String extentStr) {
        if (extentStr == null || extentStr.isBlank()) {
            throw new IllegalArgumentException("Extent must not be empty");
        }

        // 将逗号替换为空格并按任意空白拆分
        String[] parts = extentStr.replace(',', ' ').trim().split("\\s+");

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    String.format("Extent must contain exactly 4 coordinates, found %d", parts.length));
        }

        double[] extent = new double[4];
        for (int i = 0; i < 4; i++) {
            try {
                extent[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Invalid coordinate value at position %d: %s", i, parts[i]));
            }
        }

        return extent;
    }

    /**
     * 从逗号分隔的字符串中解析最大距离，并匹配到各配置方案
     * 
     * @param maxDistancesStr 逗号分隔的最大距离字符串
     * @param profiles        配置方案名称数组
     * @return 配置方案到最大距离的映射
     * @throws IllegalArgumentException 如果最大距离的数量与配置方案的数量不匹配
     */
    private Map<String, Double> parseMaxDistances(String maxDistancesStr, String[] profiles) {
        Map<String, Double> maxDistanceByProfile = new HashMap<>();

        if (maxDistancesStr != null && !maxDistancesStr.isBlank()) {
            String[] maxDistances = maxDistancesStr.split(",");
            if (maxDistances == null) {
                throw new IllegalArgumentException("Failed to split max distances string");
            }

            // 校验数量是否匹配
            if (maxDistances.length != profiles.length) {
                throw new IllegalArgumentException(String.format(
                        "Number of max distances (%d) must match number of profiles (%d)",
                        maxDistances.length, profiles.length));
            }

            // 按顺序将最大距离分配给配置方案
            for (int i = 0; i < profiles.length; i++) {
                try {
                    double maxDistance = Double.parseDouble(maxDistances[i].trim());
                    maxDistanceByProfile.put(profiles[i], maxDistance);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid max distance for profile {}: {}", profiles[i], maxDistances[i]);
                }
            }
        } else {
            throw new IllegalArgumentException("Max distances must be provided");
        }

        return maxDistanceByProfile;
    }

    private int parseNumThreads(String numThreads) {
        try {
            return Integer.parseInt(numThreads);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number of threads: " + e.getMessage());
        }
    }
}
