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

package org.heigit.geovisroute.api.requests.routing;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.heigit.geovisroute.api.APIEnums;
import org.heigit.geovisroute.api.requests.common.APIRequest;
import org.heigit.geovisroute.exceptions.ParameterValueException;
import org.heigit.geovisroute.routing.RouteRequestParameterNames;
import org.heigit.geovisroute.routing.RoutingErrorCodes;
import org.heigit.geovisroute.routing.RoutingProfileType;
import org.locationtech.jts.geom.Coordinate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.heigit.geovisroute.api.services.ApiService.convertRouteProfileType;

@Schema(title = "Directions Service", name = "directionsService", description = "发送至路线规划服务的 JSON 请求体，用于定义生成路线的相关选项和参数。")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RouteRequest extends APIRequest implements RouteRequestParameterNames {

    @Schema(name = PARAM_COORDINATES, description = "路线使用的途经点，以 WGS 84 (EPSG:4326) 的 `longitude/latitude` 对数组表示",
            example = "[[8.681495,49.41461],[8.686507,49.41943],[8.687872,49.420318]]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(PARAM_COORDINATES)
    private List<List<Double>> coordinates;

    //TODO (GTFS)：我们可能需要让这些参数中的一部分仅在选择了 pt 配置方案时才有效。

    @Schema(name = PARAM_PREFERENCE,
            description = "指定路线偏好。",
            defaultValue = "recommended")
    @JsonProperty(value = PARAM_PREFERENCE)
    private APIEnums.RoutePreference routePreference;
    @JsonIgnore
    private boolean hasRoutePreference = false;

    @Schema(name = PARAM_FORMAT, hidden = true)
    @JsonProperty(PARAM_FORMAT)
    private APIEnums.RouteResponseType responseType = APIEnums.RouteResponseType.JSON;

    @Schema(name = PARAM_UNITS,
            description = "指定距离单位。",
            defaultValue = "m")
    @JsonProperty(value = PARAM_UNITS)
    private APIEnums.Units units;
    @JsonIgnore
    private boolean hasUnits = false;

    @Schema(name = PARAM_LANGUAGE,
            description = "路线导航指示所用的语言。",
            defaultValue = "en")
    @JsonProperty(value = PARAM_LANGUAGE)
    private APIEnums.Languages language;
    @JsonIgnore
    private boolean hasLanguage = false;

    @Schema(name = PARAM_GEOMETRY,
            description = "指定是否返回几何。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "format"),
                    @ExtensionProperty(name = "value", value = "[\"json\"]", parseValue = true)}
            )},
            defaultValue = "true")
    @JsonProperty(value = PARAM_GEOMETRY)
    private boolean includeGeometry;
    @JsonIgnore
    private boolean hasIncludeGeometry = false;

    @Schema(name = PARAM_INSTRUCTIONS,
            description = "指定是否返回导航指示。",
            defaultValue = "true")
    @JsonProperty(value = PARAM_INSTRUCTIONS)
    private boolean includeInstructionsInResponse;
    @JsonIgnore
    private boolean hasIncludeInstructions = false;

    @Schema(name = PARAM_INSTRUCTIONS_FORMAT,
            description = "选择 html 可获得更详细的导航指示。",
            defaultValue = "text")
    @JsonProperty(value = PARAM_INSTRUCTIONS_FORMAT)
    private APIEnums.InstructionsFormat instructionsFormat;
    @JsonIgnore
    private boolean hasInstructionsFormat = false;

    @Schema(name = PARAM_ROUNDABOUT_EXITS,
            description = "提供环岛入口以及所经过的所有环岛出口的方位角。会在响应的 step 对象中增加 `exit_bearings` 数组。 ",
            defaultValue = "false")
    @JsonProperty(value = PARAM_ROUNDABOUT_EXITS)
    private boolean includeRoundaboutExitInfo;
    @JsonIgnore
    private boolean hasIncludeRoundaboutExitInfo = false;

    @Schema(name = PARAM_ATTRIBUTES,
            description = "路线属性列表",
            example = "[\"avgspeed\",\"percentage\"]")
    @JsonProperty(PARAM_ATTRIBUTES)
    private APIEnums.Attributes[] attributes;
    @JsonIgnore
    private boolean hasAttributes = false;

    @Schema(name = PARAM_MANEUVERS, description = "指定 step 对象中是否包含 maneuver 对象。 ",
            defaultValue = "false")
    @JsonProperty(value = PARAM_MANEUVERS)
    private boolean includeManeuvers;
    @JsonIgnore
    private boolean hasIncludeManeuvers = false;

    @Schema(name = PARAM_RADII, description = """
            限制每个给定途经点在搜索附近路段时的最大距离（单位：米）列表。 \
            取值必须大于 0，-1 表示使用最大可能的搜索半径。半径的数量应与途经点数量一致。若只给定一个值，则应用于所有途经点。\
            """,
            example = "[200, -1, 30]")
    @JsonProperty(PARAM_RADII)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private Double[] maximumSearchRadii;
    @JsonIgnore
    private boolean hasMaximumSearchRadii = false;

    @Schema(name = PARAM_BEARINGS, description = """
            指定一组（方位角与偏差）对，用于过滤途经点可以吸附到的路网路段。
            例如 `bearings=[[45,10],[120,20]]`。
            每一对是由逗号分隔的列表，可包含一个或两个浮点值：第一个是方位角，第二个是允许偏离的偏差范围。
            方位角取值为从真北顺时针 `0` 到 `360`。若未设置偏差，则默认使用 `100` 度。
            对的数量必须与途经点数量一致。
            方位角的数量与 waypoints-1 或 waypoints 的长度对应。若给定了最后一个途经点的方位信息，则用它控制到达目的途经点时可从哪个扇区进入。
            可以为某个途经点跳过方位角设置，即传入空数组，例如 `[30,20],[],[40,20]`。""",
            example = "[[30, 20], [], [40, 20]]",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "cycling-*")}
            )}
    )
    @JsonProperty(PARAM_BEARINGS)
    private Double[][] bearings;
    @JsonIgnore
    private boolean hasBearings = false;

    @Schema(name = PARAM_CONTINUE_STRAIGHT,
            description = "强制路线在途经点处保持直行，禁止在那里掉头，即使掉头会更快。",
            defaultValue = "false")
    @JsonProperty(value = PARAM_CONTINUE_STRAIGHT)
    private boolean continueStraightAtWaypoints;
    @JsonIgnore
    private boolean hasContinueStraightAtWaypoints = false;

    @Schema(name = PARAM_ELEVATION,
            description = "指定是否返回各点的高程值。请注意，对于 JSON 响应中编码的 polyline，高程也会被编码。",
            example = "false")
    @JsonProperty(value = PARAM_ELEVATION)
    private boolean useElevation;
    @JsonIgnore
    private boolean hasUseElevation = false;

    @Schema(name = PARAM_EXTRA_INFO,
            description = "响应中需要包含的附加信息项",
            example = "[\"waytype\",\"surface\"]")
    @JsonProperty(PARAM_EXTRA_INFO)
    private APIEnums.ExtraInfo[] extraInfo;
    @JsonIgnore
    private boolean hasExtraInfo = false;

    @Schema(name = PARAM_OPTIMIZED, description = "在可用时使用 CH（收缩层级）。 ",
            defaultValue = "true",
            hidden = true)
    @JsonProperty(value = PARAM_OPTIMIZED)
    private boolean useContractionHierarchies;
    @JsonIgnore
    private boolean hasUseContractionHierarchies = false;

    @Schema(name = PARAM_OPTIONS,
            description = "高级选项，格式为 JSON 对象。结构请参考[这些示例](https://giscience.github.io/openrouteservice/api-reference/endpoints/directions/routing-options#examples)。",
            example = "{\"avoid_borders\":\"controlled\"}")
    @JsonProperty(PARAM_OPTIONS)
    private RouteRequestOptions routeOptions;
    @JsonIgnore
    private boolean hasRouteOptions = false;

    @Schema(name = PARAM_SUPPRESS_WARNINGS,
            description = "抑制响应中的警告消息",
            example = "false")
    @JsonProperty(PARAM_SUPPRESS_WARNINGS)
    private boolean suppressWarnings;
    @JsonIgnore
    private boolean hasSuppressWarnings = false;

    @Schema(name = PARAM_SIMPLIFY_GEOMETRY, description = """
            指定是否简化几何。 \
            对于包含多个**分段**的路线，或在需要 `extra_info` 时，不能简化几何。\
            """,
            defaultValue = "false")
    @JsonProperty(value = PARAM_SIMPLIFY_GEOMETRY)
    private boolean simplifyGeometry;
    @JsonIgnore
    private boolean hasSimplifyGeometry = false;

    @Schema(name = PARAM_SKIP_SEGMENTS, description = """
            指定在路线计算中应跳过的分段。 \
            分段是两个给定坐标之间的连接，编号从 1 开始，对应第一个与第二个坐标之间的连接。\
            """,
            example = "[2,4]")
    @JsonProperty(PARAM_SKIP_SEGMENTS)
    private List<Integer> skipSegments;
    @JsonIgnore
    private boolean hasSkipSegments = false;

    @Schema(name = PARAM_ALTERNATIVE_ROUTES,
            description = "指定是否计算备选路线，并提供确定合适备选路线的算法参数。",
            example = "{\"target_count\":2,\"weight_factor\":1.6}")
    @JsonProperty(PARAM_ALTERNATIVE_ROUTES)
    private RouteRequestAlternativeRoutes alternativeRoutes;
    @JsonIgnore
    private boolean hasAlternativeRoutes = false;

    @Schema(name = PARAM_DEPARTURE, description = "以本地时区提供的出发日期和时间",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "format"),
                    @ExtensionProperty(name = "valueNot", value = "[\"*\"]", parseValue = true)}
            )},
            example = "2020-01-31T12:45:00", hidden = true)
    @JsonProperty(PARAM_DEPARTURE)
    private LocalDateTime departure;
    @JsonIgnore
    private boolean hasDeparture = false;

    @Schema(name = PARAM_ARRIVAL, description = "以本地时区提供的到达日期和时间",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "format"),
                    @ExtensionProperty(name = "valueNot", value = "[\"*\"]", parseValue = true)}
            )},
            example = "2020-01-31T13:15:00", hidden = true)
    @JsonProperty(PARAM_ARRIVAL)
    private LocalDateTime arrival;
    @JsonIgnore
    private boolean hasArrival = false;

    @Schema(name = PARAM_MAXIMUM_SPEED, description = "用户指定的最大速度。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-*")}
            )},
            example = "90")
    @JsonProperty(PARAM_MAXIMUM_SPEED)
    private double maximumSpeed;
    @JsonIgnore
    private boolean hasMaximumSpeed = false;

    /*
     * 以下参数特定于公共交通。
     * public-transport 配置方案接受的其他参数是坐标和语言。
     */
    @Schema(name = PARAM_SCHEDULE, description = "若为 true，则返回以 <departure> 为起点、在接下来 <schedule_duration> 分钟内的公共交通时刻表。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "public-transport")}
            )},
            defaultValue = "false",
            example = "true")
    @JsonProperty(PARAM_SCHEDULE)
    private boolean schedule;
    @JsonIgnore
    private boolean hasSchedule = false;

    @Schema(name = PARAM_SCHEDULE_DURATION, description = "请求公共交通时刻表时的时间窗口。" +
            " 格式为 ISO 8601 时长：https://en.wikipedia.org/wiki/ISO_8601#Durations",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "schedule"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )},
            example = "PT30M",
            type = "String")
    @JsonProperty(PARAM_SCHEDULE_DURATION)
    private Duration scheduleDuration;
    @JsonIgnore
    private boolean hasScheduleDuration = false;

    @Schema(name = PARAM_SCHEDULE_ROWS, description = "请求时刻表时返回的最大条目数。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "schedule"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )},
            example = "3")
    @JsonProperty(PARAM_SCHEDULE_ROWS)
    private int scheduleRows;
    @JsonIgnore
    private boolean hasScheduleRows = false;

    @Schema(name = PARAM_WALKING_TIME, description = "公共交通进出站步行接驳的最长时长。" +
            " 取值以 ISO 8601 时长格式传入：https://en.wikipedia.org/wiki/ISO_8601#Durations",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "public-transport")}
            )},
            defaultValue = "PT15M",
            example = "PT30M",
            type = "String")
    @JsonProperty(PARAM_WALKING_TIME)
    private Duration walkingTime;
    @JsonIgnore
    private boolean hasWalkingTime = false;

    @Schema(name = PARAM_IGNORE_TRANSFERS, description = "指定是否忽略换乘这一评判标准。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "public-transport")}
            )},
            defaultValue = "false",
            example = "true")
    @JsonProperty(PARAM_IGNORE_TRANSFERS)
    private boolean ignoreTransfers;
    @JsonIgnore
    private boolean hasIgnoreTransfers = false;

    @Getter
    @Schema(name = PARAM_CUSTOM_MODEL, description = "指定用于权重计算的自定义模型。", example = """
    {\
        "speed": [\
            {\
                "if": true,\
                "limit_to": 100\
            }\
        ],\
        "priority": [\
            {\
                "if": "road_class == MOTORWAY",\
                "multiply_by": 0\
            }\
        ],\
        "distance_influence": 100\
    }""")
    @JsonProperty(PARAM_CUSTOM_MODEL)
    private RouteRequestCustomModel customModel;
    @JsonIgnore
    private boolean hasCustomModel = false;

    @JsonCreator
    public RouteRequest(@JsonProperty(value = PARAM_COORDINATES, required = true) List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    public RouteRequest(Double[][] coordinates) throws ParameterValueException {
        if (coordinates.length < 2)
            throw new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_FORMAT, PARAM_COORDINATES);

        this.coordinates = new ArrayList<>();
        for (Double[] coordPair : coordinates) {
            if (coordPair.length != 2)
                throw new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_FORMAT, PARAM_COORDINATES);
            List<Double> coordPairList = new ArrayList<>();
            coordPairList.add(coordPair[0]);
            coordPairList.add(coordPair[1]);
            this.coordinates.add(coordPairList);
        }
    }

    public RouteRequest(Coordinate start, Coordinate end) throws ParameterValueException {
        if (start == null) {
            throw new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_FORMAT, "start");
        }
        if (end == null) {
            throw new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_FORMAT, "end");
        }

        this.coordinates = new ArrayList<>();
        this.coordinates.add(
                new ArrayList<>(Arrays.asList(start.x, start.y))
        );
        this.coordinates.add(
                new ArrayList<>(Arrays.asList(end.x, end.y))
        );
    }

    public List<List<Double>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    public APIEnums.Units getUnits() {
        return units;
    }

    public void setUnits(APIEnums.Units units) {
        this.units = units;
        hasUnits = true;
    }

    public APIEnums.Languages getLanguage() {
        return language;
    }

    public void setLanguage(APIEnums.Languages language) {
        this.language = language;
        hasLanguage = true;
    }

    public APIEnums.RoutePreference getRoutePreference() {
        return routePreference;
    }

    public void setRoutePreference(APIEnums.RoutePreference routePreference) {
        this.routePreference = routePreference;
        this.hasRoutePreference = true;
    }

    public boolean hasRoutePreference() {
        return hasRoutePreference;
    }

    public APIEnums.RouteResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(APIEnums.RouteResponseType responseType) {
        this.responseType = responseType;
    }

    public APIEnums.ExtraInfo[] getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(APIEnums.ExtraInfo[] extraInfo) {
        this.extraInfo = extraInfo;
        this.hasExtraInfo = true;
    }

    public RouteRequestOptions getRouteOptions() {
        return routeOptions;
    }

    public void setRouteOptions(RouteRequestOptions routeOptions) {
        this.routeOptions = routeOptions;
        hasRouteOptions = true;
    }

    public boolean getIncludeGeometry() {
        return includeGeometry;
    }

    public void setIncludeGeometry(boolean includeGeometry) {
        this.includeGeometry = includeGeometry;
        this.hasIncludeGeometry = true;
    }

    public boolean getIncludeInstructionsInResponse() {
        return includeInstructionsInResponse;
    }

    public void setIncludeInstructionsInResponse(Boolean includeInstructionsInResponse) {
        this.includeInstructionsInResponse = includeInstructionsInResponse;
        hasIncludeInstructions = true;
    }

    public APIEnums.InstructionsFormat getInstructionsFormat() {
        return instructionsFormat;
    }

    public void setInstructionsFormat(APIEnums.InstructionsFormat instructionsFormat) {
        this.instructionsFormat = instructionsFormat;
        hasInstructionsFormat = true;
    }

    public boolean getIncludeRoundaboutExitInfo() {
        return includeRoundaboutExitInfo;
    }

    public void setIncludeRoundaboutExitInfo(Boolean includeRoundaboutExitInfo) {
        this.includeRoundaboutExitInfo = includeRoundaboutExitInfo;
        hasIncludeRoundaboutExitInfo = true;
    }

    public APIEnums.Attributes[] getAttributes() {
        return attributes;
    }

    public void setAttributes(APIEnums.Attributes[] attributes) {
        this.attributes = attributes;
        this.hasAttributes = true;
    }

    public boolean getIncludeManeuvers() {
        return includeManeuvers;
    }

    public void setIncludeManeuvers(Boolean includeManeuvers) {
        this.includeManeuvers = includeManeuvers;
        hasIncludeManeuvers = true;
    }

    public Double[] getMaximumSearchRadii() {
        return maximumSearchRadii;
    }

    public void setMaximumSearchRadii(Double[] maximumSearchRadii) {
        this.maximumSearchRadii = maximumSearchRadii;
        hasMaximumSearchRadii = true;
    }

    public Double[][] getBearings() {
        return bearings;
    }

    public void setBearings(Double[][] bearings) {
        this.bearings = bearings;
        hasBearings = true;
    }

    public boolean getContinueStraightAtWaypoints() {
        return continueStraightAtWaypoints;
    }

    public void setContinueStraightAtWaypoints(Boolean continueStraightAtWaypoints) {
        this.continueStraightAtWaypoints = continueStraightAtWaypoints;
        hasContinueStraightAtWaypoints = true;
    }

    public boolean getUseElevation() {
        return useElevation;
    }

    public void setUseElevation(Boolean useElevation) {
        this.useElevation = useElevation;
        hasUseElevation = true;
    }

    public boolean getUseContractionHierarchies() {
        return useContractionHierarchies;
    }

    public void setUseContractionHierarchies(Boolean useContractionHierarchies) {
        this.useContractionHierarchies = useContractionHierarchies;
        hasUseContractionHierarchies = true;
    }

    public boolean getSuppressWarnings() {
        return suppressWarnings;
    }

    public void setSuppressWarnings(boolean suppressWarnings) {
        this.suppressWarnings = suppressWarnings;
        hasSuppressWarnings = true;
    }

    public boolean getSimplifyGeometry() {
        return simplifyGeometry;
    }

    public void setSimplifyGeometry(boolean simplifyGeometry) {
        this.simplifyGeometry = simplifyGeometry;
        this.hasSimplifyGeometry = true;
    }

    public List<Integer> getSkipSegments() {
        return this.skipSegments;
    }

    public void setSkipSegments(List<Integer> skipSegments) {
        this.skipSegments = skipSegments;
        hasSkipSegments = true;
    }

    public RouteRequestAlternativeRoutes getAlternativeRoutes() {
        return alternativeRoutes;
    }

    public void setAlternativeRoutes(RouteRequestAlternativeRoutes alternativeRoutes) {
        this.alternativeRoutes = alternativeRoutes;
        hasAlternativeRoutes = true;
    }

    public LocalDateTime getDeparture() {
        return departure;
    }

    public void setDeparture(LocalDateTime departure) {
        this.departure = departure;
        hasDeparture = true;
    }

    public LocalDateTime getArrival() {
        return arrival;
    }

    public void setArrival(LocalDateTime arrival) {
        this.arrival = arrival;
        hasArrival = true;
    }

    public void setMaximumSpeed(Double maximumSpeed) {
        this.maximumSpeed = maximumSpeed;
        hasMaximumSpeed = true;
    }

    public double getMaximumSpeed() {
        return maximumSpeed;
    }

    public boolean hasIncludeRoundaboutExitInfo() {
        return hasIncludeRoundaboutExitInfo;
    }

    public boolean hasAttributes() {
        return hasAttributes;
    }

    public boolean hasIncludeManeuvers() {
        return hasIncludeManeuvers;
    }

    public boolean hasMaximumSearchRadii() {
        return hasMaximumSearchRadii;
    }

    public boolean hasBearings() {
        return hasBearings;
    }

    public boolean hasContinueStraightAtWaypoints() {
        return hasContinueStraightAtWaypoints;
    }

    public boolean hasIncludeInstructions() {
        return hasIncludeInstructions;
    }

    public boolean hasIncludeGeometry() {
        return hasIncludeGeometry;
    }

    public boolean hasLanguage() {
        return hasLanguage;
    }

    public boolean hasInstructionsFormat() {
        return hasInstructionsFormat;
    }

    public boolean hasUnits() {
        return hasUnits;
    }

    public boolean hasUseElevation() {
        return hasUseElevation;
    }

    public boolean hasRouteOptions() {
        return hasRouteOptions;
    }

    public boolean hasUseContractionHierarchies() {
        return hasUseContractionHierarchies;
    }

    public boolean hasExtraInfo() {
        return hasExtraInfo;
    }

    public boolean hasSuppressWarnings() {
        return hasSuppressWarnings;
    }

    public boolean hasSimplifyGeometry() {
        return hasSimplifyGeometry;
    }

    public boolean hasSkipSegments() {
        return hasSkipSegments;
    }

    public boolean hasAlternativeRoutes() {
        return hasAlternativeRoutes;
    }

    public boolean hasDeparture() {
        return hasDeparture;
    }

    public boolean hasArrival() {
        return hasArrival;
    }

    public boolean hasMaximumSpeed() {
        return hasMaximumSpeed;
    }

    public boolean getSchedule() {
        return schedule;
    }

    public void setSchedule(boolean schedule) {
        this.schedule = schedule;
        this.hasSchedule = true;
    }

    public boolean hasSchedule() {
        return hasSchedule;
    }

    public Duration getScheduleDuration() {
        return scheduleDuration;
    }

    public void setScheduleDuration(Duration scheduleDuration) {
        this.scheduleDuration = scheduleDuration;
        this.hasScheduleDuration = true;
    }

    public boolean hasScheduleDuration() {
        return hasScheduleDuration;
    }

    public int getScheduleRows() {
        return scheduleRows;
    }

    public void setScheduleRows(int scheduleRows) {
        this.scheduleRows = scheduleRows;
        this.hasScheduleRows = true;
    }

    public boolean hasScheduleRows() {
        return hasScheduleRows;
    }

    public Duration getWalkingTime() {
        return walkingTime;
    }

    public void setWalkingTime(Duration walkingTime) {
        this.walkingTime = walkingTime;
        this.hasWalkingTime = true;
    }

    public boolean hasWalkingTime() {
        return hasWalkingTime;
    }


    public boolean isIgnoreTransfers() {
        return ignoreTransfers;
    }

    public void setIgnoreTransfers(boolean ignoreTransfers) {
        this.ignoreTransfers = ignoreTransfers;
        this.hasIgnoreTransfers = true;
    }

    public boolean hasIgnoreTransfers() {
        return hasIgnoreTransfers;
    }

    @JsonIgnore
    public boolean isPtRequest() {
        return convertRouteProfileType(profile) == RoutingProfileType.PUBLIC_TRANSPORT;
    }

    public void setCustomModel(RouteRequestCustomModel customModel) {
        this.customModel = customModel;
        this.hasCustomModel = true;
    }

    public boolean hasCustomModel() {
        return hasCustomModel;
    }
}
