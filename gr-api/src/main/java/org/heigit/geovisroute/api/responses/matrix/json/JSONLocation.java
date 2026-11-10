/*
 * This file is part of Openrouteservice.
 *
 * Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, see <https://www.gnu.org/licenses/>.
 */

package org.heigit.geovisroute.api.responses.matrix.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.matrix.ResolvedLocation;
import org.heigit.geovisroute.util.FormatUtility;
import org.locationtech.jts.geom.Coordinate;

public class JSONLocation {
    protected static final int COORDINATE_DECIMAL_PLACES = 6;
    private static final int SNAPPED_DISTANCE_DECIMAL_PLACES = 2;

    @Schema(description = "路网上最近可通行点的 {longitude},{latitude} 坐标",
            example = "[8.678962, 49.40783]")
    @JsonProperty(value = "location")
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    protected Coordinate location;

    @Schema(description = "最近可通行点所在街道的名称。仅当 `resolve_locations=true` 且名称可用时返回。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "resolve_locations"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )},
            example = "Bergheimer Straße")
    @JsonProperty(value = "name")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected String name;

    @Schema(description = "`source/destination` 位置与路网上所用点之间的距离，单位为米。", example = "1.2")
    @JsonProperty(value = "snapped_distance")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "%.2d")
    private final Double snappedDistance;

    JSONLocation(ResolvedLocation location, boolean includeResolveLocations) {
        this.snappedDistance = location.getSnappedDistance();
        this.location = location.getCoordinate();
        if (includeResolveLocations)
            this.name = location.getName();
    }

    public Double getSnappedDistance() {
        return FormatUtility.roundToDecimals(snappedDistance, SNAPPED_DISTANCE_DECIMAL_PLACES);
    }

    public Double[] getLocation() {
        return new Double[0];
    }

    public String getName() {
        return name;
    }
}
