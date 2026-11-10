package utils;

import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class OrsApiHelper {

    private static final String REQUEST_BODY = "{\"coordinates\":[[8.681495,49.41461],[8.686507,49.41943],[8.687872,49.420318]],\"options\":{\"avoid_polygons\":{\"coordinates\":[[[8.684881031827587,49.41768066444595],[8.684881031827587,49.41699648134178],[8.685816955915811,49.41699648134178],[8.685816955915811,49.41768066444595],[8.684881031827587,49.41768066444595]]],\"type\":\"Polygon\"}}}";

    public static boolean checkAvoidAreaRequest(String url, int expectedHttpCode) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/geo+json; charset=utf-8");
            connection.setDoOutput(true);
            connection.getOutputStream().write(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));

            int responseCode = connection.getResponseCode();
            return responseCode == expectedHttpCode;
        } catch (IOException e) {
            return false;
        }
    }

    private static JsonNode getProfiles(String address, int port) throws IOException {
        // 使用地址和端口创建一个新的 URL 对象
        URL url = new URL("http://" + address + ":" + port + "/ors/v2/status");
        // 使用该 URL 创建一个新的 HTTP 连接对象
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // 连接到该 URL
        connection.connect();
        // 从连接中读取响应
        String json = new String(connection.getInputStream().readAllBytes());
        // 关闭连接
        connection.disconnect();
        // 将响应解析为 JSON 对象
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        // 从 JSON 节点中提取配置方案

        return node.get("profiles");
    }

    public static void assertProfilesLoaded(GenericContainer<?> container, Map<String, Boolean> expectedProfiles) {
        // 获取 expectedProfiles 为 true 的所有键
        ArrayList<String> expectedProfilesKeys = new ArrayList<>();
        expectedProfiles.forEach((key, value) -> {
            if (value) {
                expectedProfilesKeys.add(key);
            }
        });
        JsonNode profiles;
        try {
            profiles = OrsApiHelper.getProfiles(container.getHost(), container.getFirstMappedPort());
            Assertions.assertNotNull(profiles, "API returned no valid profiles.");
            Assertions.assertEquals(expectedProfilesKeys.size(), profiles.size());
            for (Map.Entry<String, Boolean> profile : expectedProfiles.entrySet()) {
                if (profile.getValue()) Assertions.assertTrue(profiles.has(profile.getKey()));
                else Assertions.assertFalse(profiles.has(profile.getKey()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
