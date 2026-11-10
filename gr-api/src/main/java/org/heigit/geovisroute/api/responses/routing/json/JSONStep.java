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
import org.heigit.geovisroute.routing.RouteStep;
import org.heigit.geovisroute.util.StringUtility;

import java.util.Arrays;

@Schema(name = "JSONStep", description = "路线分段的步骤")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JSONStep {
    @Schema(description = "该步骤的距离，单位为米。", example = "245")
    @JsonProperty("distance")
    private final Double distance;
    @Schema(description = "该步骤的时长，单位为秒。", example = "96.2")
    @JsonProperty("duration")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "%.1d")
    private final Double duration;
    @Schema(description = "用于符号化显示的 [instruction](https://giscience.github.io/openrouteservice/api-reference/endpoints/directions/instruction-types) 动作。", example = "1")
    @JsonProperty("type")
    private final Integer type;
    @Schema(description = "该步骤的导航指令文本。", example = "Turn right onto Berliner Straße")
    @JsonProperty("instruction")
    private final String instruction;
    @Schema(description = "下一条街道的名称。", example = "Berliner Straße")
    @JsonProperty("name")
    private String name;
    @Schema(description = "仅用于环岛。包含应驶出的出口编号。", example = "2")
    @JsonProperty("exit_number")
    private Integer exitNumber;
    @Schema(description = "包含环岛入口以及所经过各出口的方位角。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "roundabout_exits"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )}, example = "[10,45,60]")
    @JsonProperty("exit_bearings")
    private int[] exitBearings;
    @Schema(description = "包含该步骤起点和终点对应 *geometry* 的索引的列表。", example = "[45,48]")
    @JsonProperty("way_points")
    private int[] waypoints;
    @Schema(description = "需要执行的机动动作。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "maneuvers"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )})
    @JsonProperty("maneuver")
    private JSONStepManeuver maneuver;

    public JSONStep(RouteStep step) {
        this.distance = step.getDistance();
        this.duration = step.getDuration();
        this.type = step.getType();
        this.instruction = step.getInstruction();

        this.name = StringUtility.isEmpty(step.getName()) ? "-" : step.getName();

        if (step.getExitNumber() != -1)
            this.exitNumber = step.getExitNumber();

        if (step.getWayPoints().length > 0)
            this.waypoints = Arrays.copyOf(step.getWayPoints(), step.getWayPoints().length);

        if (step.getManeuver() != null)
            this.maneuver = new JSONStepManeuver(step.getManeuver());

        if (step.getRoundaboutExitBearings() != null && step.getRoundaboutExitBearings().length > 0)
            this.exitBearings = Arrays.copyOf(step.getRoundaboutExitBearings(), step.getRoundaboutExitBearings().length);
    }

    public Double getDistance() {
        return distance;
    }

    public Double getDuration() {
        return duration;
    }

    public Integer getType() {
        return type;
    }

    public String getInstruction() {
        return instruction;
    }

    public String getName() {
        return name;
    }

    public Integer getExitNumber() {
        return exitNumber;
    }

    public int[] getWaypoints() {
        return waypoints;
    }

    public JSONStepManeuver getManeuver() {
        return maneuver;
    }
}
