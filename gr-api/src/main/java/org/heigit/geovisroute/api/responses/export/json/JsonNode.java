package org.heigit.geovisroute.api.responses.export.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.util.FormatUtility;
import org.locationtech.jts.geom.Coordinate;

import java.util.Map;

public class JsonNode {
    protected static final int COORDINATE_DECIMAL_PLACES = 6;

    @Schema(description = "对应路网节点的 ID", example = "1")
    @JsonProperty(value = "nodeId")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    protected Integer nodeId;

    @Schema(description = "路网上最近可通行点的 {longitude},{latitude} 坐标",
            example = "[8.678962, 49.40783]")
    @JsonProperty(value = "location")
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    protected Coordinate location;

    JsonNode(Map.Entry<Integer, Coordinate> location) {
        this.nodeId = location.getKey();
        this.location = location.getValue();
    }

    public Double[] getLocation() {
        Double[] coord2D = new Double[2];
        coord2D[0] = FormatUtility.roundToDecimals(location.x, COORDINATE_DECIMAL_PLACES);
        coord2D[1] = FormatUtility.roundToDecimals(location.y, COORDINATE_DECIMAL_PLACES);
        // coord2D[3] = location.z; --> 第三维示例
        return coord2D;
    }
}




