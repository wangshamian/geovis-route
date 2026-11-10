package org.heigit.geovisroute.api.responses.snapping.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.responses.matrix.json.JSON2DSources;

public class GeoJSONPointGeometry {
    @JsonProperty("type")
    @Schema(description = "GeoJSON 类型", defaultValue = "Point")
    public final String type = "Point";

    @JsonProperty("coordinates")
    @Schema(description = "吸附位置的经纬度坐标", example = "[8.681495,49.41461]")
    public Double[] coordinates;

    public GeoJSONPointGeometry(JSON2DSources source) {
        this.coordinates = source.getLocation();
    }

    public String getType() {
        return type;
    }

    public Double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Double[] coordinates) {
        this.coordinates = coordinates;
    }
}
