package org.heigit.geovisroute.api.responses.common.engineinfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.json.JSONObject;

@Schema(description = "关于所使用的 openrouteservice 引擎的信息")
public class EngineInfo {
    private static final String DEFAULT_DATE = "0000-00-00T00:00:00Z";
    @Schema(description = "所查询的 openrouteservice 后端版本", example = "8.0")
    @JsonProperty("version")
    private final String version;
    @Schema(description = "服务最后更新的日期", example = "2019-02-07T14:28:11Z")
    @JsonProperty("build_date")
    private final String buildDate;
    @Schema(description = "路网数据最后更新的日期", example = "2019-02-07T14:28:11Z")
    @JsonProperty("graph_date")
    private String graphDate = DEFAULT_DATE;
    @Schema(description = "构建路网所用 OSM 数据的时间戳", example = "2019-02-07T14:28:11Z")
    @JsonProperty("osm_date")
    private String osmDate = DEFAULT_DATE;

    public EngineInfo(JSONObject infoIn) {
        version = infoIn.getString("version");
        buildDate = infoIn.getString("build_date");

        if (infoIn.has("graph_date")) {
            graphDate = infoIn.getString("graph_date");
        }

        if (infoIn.has("osm_date")) {
            osmDate = infoIn.getString("osm_date");
        }
    }
}
