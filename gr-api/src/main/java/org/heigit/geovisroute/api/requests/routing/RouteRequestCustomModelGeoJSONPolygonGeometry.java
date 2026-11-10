package org.heigit.geovisroute.api.requests.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteRequestCustomModelGeoJSONPolygonGeometry {
    @JsonProperty("type")
    @Schema(description = "GeoJSON 类型", defaultValue = "Polygon")
    public final String type = "Polygon";

    @JsonProperty("coordinates")
    public Double[][][] coordinates;
}
