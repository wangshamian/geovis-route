/*
 * This file is part of Openrouteservice.
 *
 * Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, see <https://www.gnu.org/licenses/>.
 */

package org.heigit.geovisroute.api.config;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.codehaus.commons.nullanalysis.NotNull;
import org.heigit.geovisroute.api.converters.APIRequestProfileConverter;
import org.heigit.geovisroute.api.converters.APIRequestSingleCoordinateConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new APIRequestSingleCoordinateConverter());
        registry.addConverter(new APIRequestProfileConverter());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 添加在 ors 配置中指定的访问控制设置。
     * 将来源和请求头限制为允许的值。
     * 定义要暴露的请求头以及预检结果缓存的过期时间。
     * 可用的 HTTP 方法不可配置，全局默认为 GET、POST、HEAD 和 OPTIONS。
     * 凭据被禁用。
     */
    @Bean
    public WebMvcConfigurer corsConfigurer(CorsProperties corsProps) {
        final CorsProperties corsProperties = corsProps;

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                String[] allowedMethods = new String[]{"GET", "POST", "HEAD", "OPTIONS"};
                String[] exposedHeaders = new String[]{"Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"};

                registry.addMapping("/**")
                        .allowedMethods(allowedMethods)
                        .allowCredentials(false)
                        .allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
                        .allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
                        .exposedHeaders(exposedHeaders)
                        .maxAge(corsProperties.getPreflightMaxAge());
            }
        };
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.registerModule(new JtsModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
