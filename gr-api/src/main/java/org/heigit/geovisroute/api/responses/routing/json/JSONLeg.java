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

package org.heigit.geovisroute.api.responses.routing.json;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.routing.RouteLeg;
import org.heigit.geovisroute.routing.RoutePtStop;
import org.heigit.geovisroute.routing.RouteStep;
import org.heigit.geovisroute.util.PolylineEncoder;
import org.locationtech.jts.geom.Coordinate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(name = "JSONLeg", description = "路线的行程段（leg）")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JSONLeg {
    @Schema(description = "行程段的类型，当前可选值为 'walk' 和 'pt'。", example = "pt")
    @JsonProperty("type")
    private final String type;
    @Schema(description = "行程段的出发地点。", example = "Dossenheim, Süd Bstg G1")
    @JsonProperty("departure_location")
    private final String departureLocation;
    @Schema(description = "行程段所乘公共交通车辆的方向牌（headsign）。", example = "Bismarckplatz - Speyererhof - EMBL - Boxberg - Mombertplatz")
    @JsonProperty("trip_headsign")
    private final String tripHeadsign;
    @Schema(description = "行程段的公共交通路线名称。", example = "RNV Bus 39A")
    @JsonProperty("route_long_name")
    private final String routeLongName;
    @Schema(description = "行程段的公共交通路线名称（简称）。", example = "39A")
    @JsonProperty("route_short_name")
    private final String routeShortName;
    @Schema(description = "行程段的路线描述（如 GTFS 数据集中提供）。", example = "Bus")
    @JsonProperty("route_desc")
    private final String routeDesc;
    @Schema(description = "行程段的路线类型（如 GTFS 数据集中提供）。", example = "1")
    @JsonProperty("route_type")
    private final int routeType;
    @Schema(description = "行程段的距离，单位为米。", example = "245")
    @JsonProperty("distance")
    private final Double distance;
    @Schema(description = "行程段的时长，单位为秒。", example = "96.2")
    @JsonProperty("duration")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "%.1d")
    private final Double duration;
    @Schema(description = "出发日期和时间",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "departure"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )}, example = "2020-01-31T12:45:00+01:00")
    @JsonProperty(value = "departure")
    protected ZonedDateTime departure;
    @Schema(description = "到达日期和时间",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "arrival"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )}, example = "2020-01-31T13:15:00+01:00")
    @JsonProperty(value = "arrival")
    protected ZonedDateTime arrival;
    @Schema(description = "该公共交通行程段的信息来源 feed ID。", example = "gtfs_0")
    @JsonProperty("feed_id")
    private String feedId;
    @Schema(description = "该公共交通行程段的 trip ID。", example = "trip_id: vrn-19-39A-1-2-21-H-8-Special-50-42")
    @JsonProperty("trip_id")
    private String tripId;
    @Schema(description = "该公共交通行程段的 route ID。", example = "vrn-19-39A-1")
    @JsonProperty("route_id")
    private String routeId;
    @Schema(description = "该行程段是否与上一个行程段乘坐同一车辆。", example = "false")
    @JsonProperty("is_in_same_vehicle_as_previous")
    private Boolean isInSameVehicleAsPrevious;
    @Schema(description = "行程段的几何信息。这是一个编码后的折线。", example = "yuqlH{i~s@gaUe@VgEQFcBRbB_C")
    @JsonProperty("geometry")
    @JsonUnwrapped
    private final String geomResponse;
    @Schema(description = "包含该行程段所组成的具体步骤的列表。")
    @JsonProperty("instructions")
    private final List<JSONStep> instructions;
    @Schema(description = "包含该行程段沿途各站点的列表。")
    @JsonProperty("stops")
    private final List<JSONPtStop> stops;

    public JSONLeg(RouteLeg leg) {
        type = leg.getType();
        departureLocation = leg.getDepartureLocation();
        tripHeadsign = leg.getTripHeadsign();
        routeLongName = leg.getRouteLongName();
        routeShortName = leg.getRouteShortName();
        routeDesc = leg.getRouteDesc();
        routeType = leg.getRouteType();
        distance = leg.getDistance();
        duration = leg.getDuration();
        departure = leg.getDepartureTime();
        arrival = leg.getArrivalTime();
        feedId = leg.getFeedId();
        tripId = leg.getTripId();
        routeId = leg.getRouteId();
        isInSameVehicleAsPrevious = type.equals("pt") ? leg.isInSameVehicleAsPrevious() : null;
        if (leg.getInstructions() != null) {
            instructions = new ArrayList<>();
            for (RouteStep routeStep : leg.getInstructions()) {
                instructions.add(new JSONStep(routeStep));
            }
        } else {
            instructions = null;
        }
        if (leg.getStops() != null) {
            stops = new ArrayList<>();
            for (RoutePtStop stop : leg.getStops()) {
                stops.add(new JSONPtStop(stop));
            }
        } else {
            stops = null;
        }
        geomResponse = constructEncodedGeometry(leg.getGeometry(), leg.getIncludeElevation());
    }

    private String constructEncodedGeometry(final Coordinate[] coordinates, boolean includeElevation) {
        if (coordinates != null)
            return PolylineEncoder.encode(coordinates, includeElevation, new StringBuilder());
        else
            return "";
    }
}
