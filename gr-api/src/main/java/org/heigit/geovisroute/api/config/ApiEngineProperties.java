package org.heigit.geovisroute.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ors.engine")
public class ApiEngineProperties extends org.heigit.geovisroute.config.EngineProperties {
}
