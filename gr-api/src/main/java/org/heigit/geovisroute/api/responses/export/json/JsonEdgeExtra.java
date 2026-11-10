package org.heigit.geovisroute.api.responses.export.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.common.Pair;

import java.util.Map;

public class JsonEdgeExtra {

    @Schema(description = "对应路网边的 ID", example = "1")
    @JsonProperty(value = "edgeId")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    protected String nodeId;

    @Schema(description = "存储在该边上的附加信息",
            example = "{\"surface_quality_known\" : \"true\"}")
    @JsonProperty(value = "extra")
    @JsonFormat()
    protected Object extra;

    JsonEdgeExtra(Map.Entry<Pair<Integer, Integer>, Map<String, Object>> edge) {
        this.nodeId = edge.getKey().first.toString() + "->" + edge.getKey().second.toString();
        this.extra = edge.getValue();
    }
}




