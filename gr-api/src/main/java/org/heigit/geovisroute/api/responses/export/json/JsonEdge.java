package org.heigit.geovisroute.api.responses.export.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.common.Pair;

import java.util.Map;

public class JsonEdge {
    @Schema(description = "该边起点的 ID", example = "1")
    @JsonProperty(value = "fromId")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    protected Integer fromId;

    @Schema(description = "该边终点的 ID", example = "2")
    @JsonProperty(value = "toId")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    protected Integer toId;

    @Schema(description = "该边在给定边界框内的权重",
            example = "123.45")
    @JsonProperty(value = "weight")
    @JsonSerialize(using = WeightSerializer.class)
    protected Double weight;

    JsonEdge(Map.Entry<Pair<Integer, Integer>, Double> weightedEdge) {
        this.fromId = weightedEdge.getKey().first;
        this.toId = weightedEdge.getKey().second;
        this.weight = weightedEdge.getValue();
    }
}
