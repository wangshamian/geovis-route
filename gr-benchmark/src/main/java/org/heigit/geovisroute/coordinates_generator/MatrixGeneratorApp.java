package org.heigit.geovisroute.coordinates_generator;

import org.heigit.geovisroute.coordinates_generator.cli.MatrixCommandLineParser;
import org.heigit.geovisroute.coordinates_generator.generators.CoordinateGeneratorMatrix;
import org.heigit.geovisroute.coordinates_generator.model.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * 用于生成矩阵测试坐标的命令行应用。
 * 它生成用于矩阵 API 测试的坐标，类似于
 * RouteGeneratorApp
 * 为 directions API 测试生成坐标的方式。
 */
public class MatrixGeneratorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatrixGeneratorApp.class);

    public static void main(String[] args) {
        try {
            MatrixCommandLineParser cli = new MatrixCommandLineParser(args);

            if (cli.hasHelp()) {
                cli.printHelp();
                return;
            }

            LOGGER.info("Creating coordinate generator for matrices...");
            CoordinateGeneratorMatrix generator = cli.createGenerator();

            LOGGER.info("Generating {} matrices...", generator.getNumMatrices());
            generator.generateMatrices();
            LOGGER.info("\n");

            List<Matrix> result = generator.getResult();
            LOGGER.info("Writing {} matrices to {}", result.size(), cli.getOutputFile());
            generator.writeToCSV(cli.getOutputFile());

            LOGGER.info("\n");
            LOGGER.info("Successfully generated {} matri{}",
                    result.size(),
                    result.size() != 1 ? "ces" : "x");
            LOGGER.info("Results written to: {}", cli.getOutputFile());
        } catch (NumberFormatException e) {
            LOGGER.error("Error parsing numeric arguments: {}", e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            LOGGER.error("Error writing to output file: {}", e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }
}