package org.heigit.geovisroute.coordinates_generator.generators;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 坐标生成实现的抽象基类
 */
public abstract class AbstractCoordinateGenerator {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractCoordinateGenerator.class);
    protected static final int DEFAULT_MAX_ATTEMPTS = 100;
    protected static final double COORDINATE_PRECISION = 1e-6;
    protected static final String DEFAULT_BASE_URL = "http://localhost:8082/ors";

    // 通用配置
    protected final String baseUrl;
    protected final double[] extent;
    protected final String[] profiles;
    protected final Random random;
    protected final ObjectMapper mapper;
    protected final String apiKey;

    /**
     * 创建新的坐标生成器
     * 
     * @param extent   边界框 [minX, minY, maxX, maxY]
     * @param profiles 要使用的路径规划配置方案列表
     * @param baseUrl  API 基础 URL
     * @param endpoint API 端点名称（用于日志记录）
     */
    protected AbstractCoordinateGenerator(double[] extent, String[] profiles, String baseUrl, String endpoint) {
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        validateBaseInputParameters(extent, profiles, endpoint);
        this.extent = extent;
        this.profiles = profiles.clone();
        this.random = new SecureRandom();
        this.mapper = new ObjectMapper();
        this.apiKey = getApiKey();
    }

    private void validateBaseInputParameters(double[] extent, String[] profiles, String endpoint) {
        if (extent == null || extent.length != 4)
            throw new IllegalArgumentException("Extent must contain 4 coordinates");
        if (profiles == null || profiles.length == 0)
            throw new IllegalArgumentException("Profiles must not be empty");
        if (endpoint == null || endpoint.isBlank())
            throw new IllegalArgumentException("Endpoint must not be empty");
    }

    /**
     * 从环境变量或系统属性获取 API 密钥
     */
    private String getApiKey() {
        if (!baseUrl.contains("openrouteservice.org")) {
            return "";
        }

        String orsApiAccessKey = System.getenv("ORS_API_KEY");
        if (orsApiAccessKey == null) {
            orsApiAccessKey = System.getProperty("ORS_API_KEY");
        }
        if (orsApiAccessKey == null) {
            throw new IllegalStateException("ORS_API_KEY environment variable is not set.");
        }
        return orsApiAccessKey;
    }

    /**
     * 创建 HTTP 请求头
     */
    public final Map<String, String> createHeaders() {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Accept", "application/json");
        requestHeaders.put("Content-Type", "application/json");
        if (!apiKey.isEmpty()) {
            requestHeaders.put("Authorization", apiKey);
        }
        return requestHeaders;
    }

    /**
     * 创建新的 HTTP 客户端
     */
    protected CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create().build();
    }

    /**
     * 处理 HTTP 响应
     */
    protected String processResponse(ClassicHttpResponse response) throws IOException {
        int status = response.getCode();
        if (status >= HttpStatus.SC_REDIRECTION) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Received error response: {}", new StatusLine(response));
            }
            return null;
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }

        try {
            return EntityUtils.toString(entity);
        } catch (ParseException | IOException e) {
            throw new IOException("Failed to parse response entity", e);
        }
    }

    /**
     * 将生成结果写入 CSV 文件
     */
    public abstract void writeToCSV(String filePath) throws IOException;

    /**
     * 带指定最大尝试次数的主生成方法
     */
    protected abstract void generate(int maxAttempts);

    /**
     * 获取生成结果
     */
    public abstract <T> List<T> getResult();

    /**
     * 在生成前初始化或清空集合
     */
    protected abstract void initializeCollections();

    /**
     * 使用默认最大尝试次数的主生成方法
     */
    public void generate() {
        generate(DEFAULT_MAX_ATTEMPTS);
    }
}
