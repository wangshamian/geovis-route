package org.heigit.geovisroute.api;

import jakarta.servlet.ServletContextListener;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.api.config.ApiEngineProperties;
import org.heigit.geovisroute.api.services.GraphService;
import org.heigit.geovisroute.api.servlet.listeners.GRInitContextListener;
import org.heigit.geovisroute.util.AppInfo;
import org.heigit.geovisroute.routing.RoutingProfileManagerStatus;
import org.heigit.geovisroute.util.StringUtility;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ServletComponentScan("org.heigit.geovisroute.api.servlet.listeners")
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Application extends SpringBootServletInitializer {
    private static final Logger LOG = Logger.getLogger(Application.class.getName());

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    public static void main(String[] args) {
        if (args.length > 0 && !StringUtility.isNullOrEmpty(args[0]) && !args[0].startsWith("-")) {
            System.setProperty(GREnvironmentPostProcessor.GR_CONFIG_LOCATION_PROPERTY, args[0]);
        }
        SpringApplication.run(Application.class, args);
        LOG.info("openrouteservice %s".formatted(AppInfo.getEngineInfo()));
        if (RoutingProfileManagerStatus.isShutdown()) {
            System.exit(RoutingProfileManagerStatus.hasFailed() ? 1 : 0);
        }
    }

    @Bean("orsInitContextListenerBean")
    public ServletListenerRegistrationBean<ServletContextListener> createORSInitContextListenerBean(ApiEngineProperties apiEngineProperties, GraphService graphService) {
        ServletListenerRegistrationBean<ServletContextListener> bean = new ServletListenerRegistrationBean<>();
        bean.setListener(new GRInitContextListener(apiEngineProperties, graphService));
        return bean;
    }
}
