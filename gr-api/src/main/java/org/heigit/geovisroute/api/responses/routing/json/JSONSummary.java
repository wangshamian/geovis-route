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
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.routing.RouteResult;

import java.util.Objects;


@Schema(description = "包含时长、路线距离和路线实际距离的总计。")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JSONSummary {
    @Schema(description = "路线总距离，使用指定的单位。", example = "12.6")
    @JsonProperty(value = "distance")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "%.2d")
    protected Double distance;
    @Schema(description = "总时长，单位为秒。", example = "604")
    @JsonProperty(value = "duration")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "%.1d")
    protected Double duration;
    @Schema(description = "累计上升高程，单位为米。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "elevation"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )}, example = "166.3")
    @JsonProperty(value = "ascent")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "%.1d")
    protected Double ascent;
    @Schema(description = "累计下降高程，单位为米。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "elevation"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )}, example = "201.3")
    @JsonProperty(value = "descent")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "%.1d")
    protected Double descent;

    @JsonProperty(value = "transfers")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = NotMinusOne.class)
    protected int transfers = -1;

    @JsonProperty(value = "fare")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = NotMinusOne.class)
    protected int fare = -1;

    public void setTransfers(int transfers) {
        this.transfers = transfers;
    }

    public void setFare(int fare) {
        this.fare = fare;
    }

    public JSONSummary(Double distance, Double duration) {
        this.distance = distance;
        this.duration = duration;
    }

    public JSONSummary(Double distance, Double duration, Double ascent, Double descent) {
        this(distance, duration);
        this.ascent = ascent;
        this.descent = descent;
    }

    public JSONSummary(RouteResult route, boolean includeElevation, boolean isPtRequest) {
        if (includeElevation) {
            this.ascent = route.getSummary().getAscent();
            this.descent = route.getSummary().getDescent();
        }
        if (isPtRequest) {
            this.transfers = route.getSummary().getTransfers();
            this.fare = route.getSummary().getFare();
        }
        this.distance = route.getSummary().getDistance();
        this.duration = route.getSummary().getDuration();
    }

    public Double getDistance() {
        return distance;
    }

    public Double getDuration() {
        return duration;
    }

    public Double getDescent() {
        return descent;
    }

    public Double getAscent() {
        return ascent;
    }

    public static class NotMinusOne {
        @Override
        public boolean equals(Object other) {
            return Objects.equals(other, -1);
        }
    }
}
