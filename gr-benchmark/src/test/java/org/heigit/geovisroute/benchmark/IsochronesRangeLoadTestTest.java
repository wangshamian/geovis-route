package org.heigit.geovisroute.benchmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.Session;
import org.heigit.geovisroute.benchmark.BenchmarkEnums.RangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IsochronesLoadTestTest {
    private ObjectMapper objectMapper;
    private Session mockSession;
    private Config mockConfig;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        objectMapper = new ObjectMapper();
        mockSession = mock(Session.class);
        mockConfig = mock(Config.class);

        // 更新 mock 设置以处理列表
        when(mockSession.get("longitude")).thenReturn(Arrays.asList(8.681495, 8.681495));
        when(mockSession.get("latitude")).thenReturn(Arrays.asList(49.41461, 49.41461));
        when(mockConfig.getFieldLon()).thenReturn("longitude");
        when(mockConfig.getFieldLat()).thenReturn("latitude");
        when(mockConfig.getRanges()).thenReturn(Arrays.asList(300, 600, 900));
    }

    @Test
    void createRequestBody_ShouldCreateValidJson() {
        // 准备
        Config config = new Config();

        // 执行
        String result = IsochronesRangeLoadTest.createRequestBody(mockSession, 1, config, RangeType.TIME);

        // 验证
        assertThat(result)
                .contains("\"locations\":[[8.681495,49.41461]]")
                .contains("\"range\":[300]")
                .contains("\"range_type\":\"time\"");
    }

    @Test
    void createRequestBody_ShouldCreateValidJsonForDistance() {
        // 准备
        Config config = new Config();

        // 执行
        String result = IsochronesRangeLoadTest.createRequestBody(mockSession, 1, config, RangeType.DISTANCE);

        // 验证
        assertThat(result)
                .contains("\"locations\":[[8.681495,49.41461]]")
                .contains("\"range\":[300]")
                .contains("\"range_type\":\"distance\"");
    }

    @Test
    void createRequestBody_ShouldIncludeMultipleLocations() {
        // 准备
        Config config = new Config();

        // 执行
        String result = IsochronesRangeLoadTest.createRequestBody(mockSession, 2, config, RangeType.TIME);

        // 验证
        assertThat(result).contains("\"locations\":[[8.681495,49.41461],[8.681495,49.41461]]");
    }

    @Test
    void testCreateRequestBodySingleLocation() throws JsonProcessingException {
        String requestBody = IsochronesRangeLoadTest.createRequestBody(mockSession, 1, mockConfig, RangeType.TIME);
        JsonNode json = objectMapper.readTree(requestBody);

        assertEquals(1, json.get("locations").size());
        assertEquals(8.681495, json.get("locations").get(0).get(0).asDouble());
        assertEquals(49.41461, json.get("locations").get(0).get(1).asDouble());
        assertEquals(300, json.get("range").get(0).asInt());
    }

    @Test
    void testCreateRequestBodyMultipleLocations() throws JsonProcessingException {
        // 设置 mock 返回三个坐标
        when(mockSession.get("longitude")).thenReturn(Arrays.asList(8.681495, 8.681495, 8.681495));
        when(mockSession.get("latitude")).thenReturn(Arrays.asList(49.41461, 49.41461, 49.41461));
        when(mockConfig.getRange()).thenReturn("500");

        String requestBody = IsochronesRangeLoadTest.createRequestBody(mockSession, 3, mockConfig, RangeType.TIME);
        JsonNode json = objectMapper.readTree(requestBody);

        assertEquals(3, json.get("locations").size());
        for (int i = 0; i < 3; i++) {
            assertEquals(8.681495, json.get("locations").get(i).get(0).asDouble());
            assertEquals(49.41461, json.get("locations").get(i).get(1).asDouble());
        }
        assertEquals(300, json.get("range").get(0).asInt());
    }

    // 用新测试替换旧的 testCreateLocationsList
    @Test
    void testCreateLocationsListFromArrays() {
        List<List<Double>> locations = IsochronesRangeLoadTest.createLocationsListFromArrays(mockSession, 2, mockConfig);

        assertEquals(2, locations.size());
        locations.forEach(coord -> {
            assertEquals(8.681495, coord.get(0));
            assertEquals(49.41461, coord.get(1));
        });
    }

    @Test
    void testCreateLocationsListFromArrays_WithNullValues() {
        when(mockSession.get("longitude")).thenReturn(null);
        when(mockSession.get("latitude")).thenReturn(Arrays.asList(49.41461));

        List<List<Double>> locations = IsochronesRangeLoadTest.createLocationsListFromArrays(mockSession, 2, mockConfig);

        assertTrue(locations.isEmpty());
    }

    @Test
    void createRequestBody_ShouldCreateValidJsonWithMultipleRanges() throws JsonProcessingException {
        // 准备
        when(mockConfig.getRanges()).thenReturn(Arrays.asList(300, 600, 900));

        // 执行
        String result = IsochronesRangeLoadTest.createRequestBody(mockSession, 1, mockConfig, RangeType.TIME);
        JsonNode json = objectMapper.readTree(result);

        // 验证
        assertThat(json.get("range").size()).isEqualTo(3);
    }
}