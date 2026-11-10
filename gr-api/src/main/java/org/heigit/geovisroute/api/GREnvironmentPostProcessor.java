package org.heigit.geovisroute.api;

import org.apache.commons.logging.Log;
import org.heigit.geovisroute.util.StringUtility;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class GREnvironmentPostProcessor implements EnvironmentPostProcessor {

    public static final String GR_CONFIG_LOCATION_ENV = "GR_CONFIG_LOCATION";
    public static final String GR_CONFIG_LOCATION_PROPERTY = "gr.config.location";
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    private final Log log;

    public GREnvironmentPostProcessor(DeferredLogFactory logFactory) {
        log = logFactory.getLog(GREnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.matchesProfiles("test")) {
            log.info("No additional configuration loaded, test profile is active.");
            return;
        }
        // 使用自定义配置 yml 文件的内容覆盖 application.yml 中的值。
        List<String> configLocations = new ArrayList<>();
        log.info("");
        log.info("Configuration lookup started.");
        if (!StringUtility.isNullOrEmpty(System.getProperty(GR_CONFIG_LOCATION_PROPERTY))) {
            configLocations.add(System.getProperty(GR_CONFIG_LOCATION_PROPERTY));
            log.info("Configuration file set by program argument.");
        }
        if (configLocations.isEmpty() && !StringUtility.isNullOrEmpty(System.getenv(GR_CONFIG_LOCATION_ENV))) {
            String configPath = System.getenv(GR_CONFIG_LOCATION_ENV);
            File configFile = new File(configPath);
            // 检查文件是否存在并记录相应的消息
            if (configFile.exists()) {
                log.info("ORS config file found at: " + configFile.getAbsolutePath());
                configLocations.add(configPath);
                log.info("Configuration file set by environment variable.");
            } else {
                // 仍然添加它以触发错误并停止 ORS
                configLocations.add(configPath);
                log.error("ORS config file not found at: " + configPath);
            }
        }
        if (configLocations.isEmpty()) {
            String home = System.getProperty("user.home");
            configLocations.add("./gr-config.yml");
            configLocations.add(home + "/.config/openrouteservice/gr-config.yml");
            configLocations.add("/etc/openrouteservice/gr-config.yml");
            log.info("Configuration file lookup by default locations.");
        }
        for (String path : configLocations) {
            try {
                List<PropertySource<?>> sources = this.loader.load("yml config", new FileSystemResource(path));
                if (!sources.isEmpty()) {
                    environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, sources.get(0));
                    System.setProperty(GR_CONFIG_LOCATION_PROPERTY, path);
                    log.info("Loaded file '%s'.".formatted(path));
                    break;
                }
            } catch (IllegalStateException | IOException ignored) {
                log.debug("Config file '%s' not found.".formatted(path));
            }
        }
        var relevantPrefixes = List.of(
                "ORS_",
                "ors.",
                "GR_",
                "gr.",
                "LOGGING_",
                "logging.",
                "SPRINGDOC_",
                "springdoc.",
                "SPRING_",
                "spring.",
                "SERVER_",
                "server.");
        var relevantENVs = environment.getSystemProperties().entrySet().stream()
                .filter(env -> relevantPrefixes.stream().anyMatch(env.getKey()::startsWith))
                .sorted(Entry.comparingByKey()).toList();
        if (!relevantENVs.isEmpty()) {
            log.info("");
            log.info("Environment variables overriding openrouteservice configuration parameters detected: ");
            relevantENVs.forEach(env -> log.info("%s=%s".formatted(env.getKey(), env.getValue())));
        }
        log.info("Configuration lookup finished.");
        log.info("");
    }
}
