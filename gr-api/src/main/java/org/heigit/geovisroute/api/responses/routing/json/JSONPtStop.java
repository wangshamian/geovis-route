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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.routing.RoutePtStop;

import java.util.Date;
import java.util.List;

@Schema(name = "JSONPtStop", description = "公共交通行程段的站点")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JSONPtStop {
    @Schema(description = "站点的 ID。", example = "de:08221:1138:0:O")
    @JsonProperty("stop_id")
    private final String stopId;
    @Schema(description = "站点的名称。", example = "Heidelberg, Alois-Link-Platz")
    @JsonProperty("name")
    private String name;
    @Schema(description = "站点的位置。", example = "[8.6912542, 49.399979]")
    @JsonProperty("location")
    private List<Double> location;
    @Schema(description = "站点的到达时间。", example = "2022-07-04T13:22:00Z")
    @JsonProperty("arrival_time")
    private Date arrivalTime;
    @Schema(description = "站点的计划到达时间。", example = "2022-07-04T13:22:00Z")
    @JsonProperty("planned_arrival_time")
    private Date plannedArrivalTime;
    @Schema(description = "站点的预计到达时间。", example = "2022-07-04T13:22:00Z")
    @JsonProperty("predicted_arrival_time")
    private Date predictedArrivalTime;
    @Schema(description = "该站点的到达是否已取消。", example = "false")
    @JsonProperty("arrival_cancelled")
    private Boolean arrivalCancelled;
    @Schema(description = "站点的出发时间。", example = "2022-07-04T13:22:00Z")
    @JsonProperty("departure_time")
    private Date departureTime;
    @Schema(description = "站点的计划出发时间。", example = "2022-07-04T13:22:00Z")
    @JsonProperty("planned_departure_time")
    private Date plannedDepartureTime;
    @Schema(description = "站点的预计出发时间。", example = "2022-07-04T13:22:00Z")
    @JsonProperty("predicted_departure_time")
    private Date predictedDepartureTime;
    @Schema(description = "该站点的出发是否已取消。", example = "false")
    @JsonProperty("departure_cancelled")
    private Boolean departureCancelled;

    public JSONPtStop(RoutePtStop stop) {
        stopId = stop.getStopId();
        name = stop.getStopName();
        location = stop.getLocationAsCoordinateList();
        arrivalTime = stop.getArrivalTime();
        plannedArrivalTime = stop.getPlannedArrivalTime();
        predictedArrivalTime = stop.getPredictedArrivalTime();
        arrivalCancelled = stop.isArrivalCancelled();
        departureTime = stop.getDepartureTime();
        plannedDepartureTime = stop.getPlannedDepartureTime();
        predictedDepartureTime = stop.getPredictedDepartureTime();
        departureCancelled = stop.isDepartureCancelled();
    }
}
