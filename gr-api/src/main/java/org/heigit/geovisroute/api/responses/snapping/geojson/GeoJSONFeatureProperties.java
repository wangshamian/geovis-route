package org.heigit.geovisroute.api.responses.snapping.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.responses.matrix.json.JSON2DSources;

public class GeoJSONFeatureProperties {
    @JsonProperty("name")
    @Schema(description = "\"最近可通行点所在街道的名称。仅当 `resolve_locations=true` 且名称可用时返回。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "resolve_locations"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )},
            example = "Gerhart-Hauptmann-Straße")
    public String name;
    @JsonProperty("snapped_distance")
    @Schema(description = "`source/destination` 位置与路网上所用点之间的距离，单位为米。",
    example = "0.02")
    public double dist;

    @JsonProperty("source_id")
    @Schema(description = "所请求位置的索引")
    public int sourceId;

    public GeoJSONFeatureProperties(int sourceId, JSON2DSources source) {
        this.sourceId = sourceId;
        this.dist = source.getSnappedDistance();
        this.name = source.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }
}
