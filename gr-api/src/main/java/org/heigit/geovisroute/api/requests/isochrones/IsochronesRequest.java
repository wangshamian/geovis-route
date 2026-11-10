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

package org.heigit.geovisroute.api.requests.isochrones;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.requests.common.APIRequest;
import org.heigit.geovisroute.api.requests.routing.RouteRequestOptions;
import org.heigit.geovisroute.exceptions.ParameterValueException;
import org.heigit.geovisroute.isochrones.IsochroneMapCollection;
import org.heigit.geovisroute.isochrones.IsochroneRequest;
import org.heigit.geovisroute.isochrones.IsochronesErrorCodes;
import org.heigit.geovisroute.api.APIEnums;
import org.heigit.geovisroute.routing.RoutingProfileType;

import java.time.LocalDateTime;
import java.util.List;

import static org.heigit.geovisroute.api.services.ApiService.convertAPIEnumListToStrings;


@Schema(name = "IsochronesRequest", description = "发送至等时圈服务的 JSON 请求体，用于定义生成等时圈的相关选项和参数。")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class IsochronesRequest extends APIRequest {
    public static final String PARAM_LOCATIONS = "locations";
    public static final String PARAM_LOCATION_TYPE = "location_type";
    public static final String PARAM_OPTIONS = "options";
    public static final String PARAM_RANGE = "range";
    public static final String PARAM_RANGE_TYPE = "range_type";
    public static final String PARAM_RANGE_UNITS = "units";
    public static final String PARAM_AREA_UNITS = "area_units";
    public static final String PARAM_INTERSECTIONS = "intersections";
    public static final String PARAM_ATTRIBUTES = "attributes";
    public static final String PARAM_INTERVAL = "interval";
    public static final String PARAM_SMOOTHING = "smoothing";
    public static final String PARAM_TIME = "time";

    @Schema(name = PARAM_LOCATIONS, description = "路线使用的位置，以 WGS 84 (EPSG:4326) 的 `longitude/latitude` 对数组表示",
            example = "[[8.681495,49.41461],[8.686507,49.41943]]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(PARAM_LOCATIONS)
    private Double[][] locations = new Double[][]{};
    @JsonIgnore
    private boolean hasLocations = false;

    @Schema(name = PARAM_LOCATION_TYPE, description = "`start` 将位置视为起点，`destination` 视为目标点。",
            defaultValue = "start")
    @JsonProperty(value = PARAM_LOCATION_TYPE)
    private IsochronesRequestEnums.LocationType locationType;
    @JsonIgnore
    private boolean hasLocationType = false;

    @Schema(name = PARAM_RANGE, description = """
            分析的最大范围值：时间以**秒**为单位，距离以**米**为单位。\
            也可以提供逗号分隔的具体范围值列表。所有位置的范围相同。\
            """,
            example = "[ 300, 200 ]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(PARAM_RANGE)
    private List<Double> range;
    @JsonIgnore
    private boolean hasRange = false;

    @Schema(name = PARAM_RANGE_TYPE,
            description = "指定等时圈的可达性类型。", defaultValue = "time")
    @JsonProperty(value = PARAM_RANGE_TYPE, defaultValue = "time")
    private IsochronesRequestEnums.RangeType rangeType;
    @JsonIgnore
    private boolean hasRangeType = false;

    // 单位仅对 range_type 为 distance 时有效，range_time 时将被忽略
    @Schema(name = PARAM_RANGE_UNITS,
            description = """
                    仅当 `range_type` 设为 distance 时指定距离单位。
                    默认值：m。 \
                    """,
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "range_type"),
                    @ExtensionProperty(name = "value", value = "distance")}
            )},
            defaultValue = "m")
    @JsonProperty(value = PARAM_RANGE_UNITS)
    private APIEnums.Units rangeUnit;
    @JsonIgnore
    private boolean hasRangeUnits = false;

    @Schema(name = PARAM_OPTIONS,
            description = "等时圈请求的附加选项",
            example = "{\"avoid_borders\":\"all\"}")
    @JsonProperty(PARAM_OPTIONS)
    private RouteRequestOptions isochronesOptions;
    @JsonIgnore
    private boolean hasOptions = false;

    @Schema(hidden = true)
    private APIEnums.RouteResponseType responseType = APIEnums.RouteResponseType.GEOJSON;

    @Schema(name = PARAM_AREA_UNITS,
            description = """
                    指定面积单位。
                    默认值：m。 \
                    """,
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "attributes"),
                    @ExtensionProperty(name = "value", value = "area")}
            )},
            defaultValue = "m")
    @JsonProperty(value = PARAM_AREA_UNITS)
    private APIEnums.Units areaUnit;
    @JsonIgnore
    private boolean hasAreaUnits = false;

    @Schema(name = PARAM_INTERSECTIONS,
            description = "指定是否返回相交的多边形。 ", defaultValue = "false")
    @JsonProperty(value = PARAM_INTERSECTIONS)
    private boolean intersections;
    @JsonIgnore
    private boolean hasIntersections = false;

    @Schema(name = PARAM_ATTRIBUTES, description = """
            等时圈属性列表
            - `area` - 等时圈多边形的面积
            - `reachfactor` - 0 到 1 之间的可达性评分
            该因子按以下方式计算：以平均速度对应半径为半径的圆面积，与等时圈多边形面积之比。
            - `total_pop` - 多边形内的总人口
            所用数据为欧盟委员会的 [Global Human Settlement Layer (GHSL)](https://ghsl.jrc.ec.europa.eu/ghs_pop2023.php)。 \
            请注意，该数据集虽于 2023 年发布，但其中包含的最新数据为 2020 年。openrouteservice 以 100m 分辨率使用该数据。 \
            """,
            example = "[\"area\"]")
    @JsonProperty(PARAM_ATTRIBUTES)
    private IsochronesRequestEnums.Attributes[] attributes;
    @JsonIgnore
    private boolean hasAttributes = false;

    @Schema(name = PARAM_INTERVAL, description = """
            等时圈或等距线的间隔。仅在给定单个范围值时使用。 \
            时间以**秒**为单位，距离以**米**为单位。\
            """,
            example = "30"
    )
    @JsonProperty(PARAM_INTERVAL)
    private Double interval;
    @JsonIgnore
    private boolean hasInterval = false;

    @Schema(name = PARAM_SMOOTHING,
            description = """
                    对生成的等时圈多边形应用一定程度的概化，通过 `0` 到 `100.0` 之间的 `smoothing_factor` 控制。
                    概化通过确定包含多边形外侧两点之间连线的最大长度来实现。
                    如果该距离大于阈值，则移除两点之间的连线，改用其它点之间更短的连线。
                    请注意，该连线的最小长度约为 1333m，因此当 `smoothing_factor` 计算出的距离小于该值时，使用最小值。
                    阈值按 `(maximum_radius_of_isochrone / 100) * smoothing_factor` 计算。
                    因此，取值越接近 100，形状越概化。
                    多边形生成算法基于 Duckham 等人（2008）`"Efficient generation of simple polygons for characterizing the shape of a set of points in the plane."`""",
            example = "25")
    @JsonProperty(value = PARAM_SMOOTHING)
    private Double smoothing;
    @JsonIgnore
    private boolean hasSmoothing = false;

    @Schema(name = PARAM_TIME, description = "以本地时区提供的出发日期和时间",
            example = "2020-01-31T12:45:00", hidden = true)
    @JsonProperty(PARAM_TIME)
    private LocalDateTime time;
    @JsonIgnore
    private boolean hasTime = false;

    @JsonIgnore
    private IsochroneMapCollection isoMaps;
    @JsonIgnore
    private IsochroneRequest isochroneRequest;

    @JsonCreator
    public IsochronesRequest() {
    }

    public static String[] convertAttributes(IsochronesRequestEnums.Attributes[] attributes) {
        return convertAPIEnumListToStrings(attributes);
    }

    public static int convertToIsochronesProfileType(APIEnums.Profile profile) throws ParameterValueException {
        try {
            int profileFromString = RoutingProfileType.getFromString(profile.toString());
            if (profileFromString == 0) {
                throw new ParameterValueException(IsochronesErrorCodes.INVALID_PARAMETER_VALUE, "profile");
            }
            return profileFromString;
        } catch (Exception e) {
            throw new ParameterValueException(IsochronesErrorCodes.INVALID_PARAMETER_VALUE, "profile");
        }
    }

    public APIEnums.Units getAreaUnit() {
        return areaUnit;
    }

    public void setAreaUnit(APIEnums.Units areaUnit) {
        this.areaUnit = areaUnit;
        hasAreaUnits = true;
    }

    public boolean hasAreaUnits() {
        return hasAreaUnits;
    }

    public Double getSmoothing() {
        return smoothing;
    }

    public void setSmoothing(Double smoothing) {
        this.smoothing = smoothing;
        this.hasSmoothing = true;
    }

    public boolean hasSmoothing() {
        return hasSmoothing;
    }

    public APIEnums.RouteResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(APIEnums.RouteResponseType responseType) {
        this.responseType = responseType;
    }

    public boolean getIntersections() {
        return intersections;
    }

    public void setIntersections(Boolean intersections) {
        this.intersections = intersections;
        hasIntersections = true;
    }

    public boolean hasIntersections() {
        return hasIntersections;
    }

    public APIEnums.Units getRangeUnit() {
        return rangeUnit;
    }

    public void setRangeUnit(APIEnums.Units rangeUnit) {
        this.rangeUnit = rangeUnit;
        hasRangeUnits = true;
    }

    public boolean hasRangeUnits() {
        return hasRangeUnits;
    }

    public IsochronesRequestEnums.Attributes[] getAttributes() {
        return attributes;
    }

    public void setAttributes(IsochronesRequestEnums.Attributes[] attributes) {
        this.attributes = attributes;
        this.hasAttributes = true;
    }

    public boolean hasAttributes() {
        return hasAttributes;
    }

    public Double[][] getLocations() {
        return locations;
    }

    public void setLocations(Double[][] locations) {
        this.locations = locations;
        hasLocations = true;
    }

    public boolean hasLocations() {
        return hasLocations;
    }

    public IsochronesRequestEnums.LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(IsochronesRequestEnums.LocationType locationType) {
        this.locationType = locationType;
        hasLocationType = true;
    }

    public boolean hasLocationType() {
        return hasLocationType;
    }

    public RouteRequestOptions getIsochronesOptions() {
        return isochronesOptions;
    }

    public void setIsochronesOptions(RouteRequestOptions isochronesOptions) {
        this.isochronesOptions = isochronesOptions;
        this.hasOptions = true;
    }

    public boolean hasOptions() {
        return this.hasOptions;
    }

    public List<Double> getRange() {
        return range;
    }

    public void setRange(List<Double> range) {
        this.range = range;
        hasRange = true;
    }

    public boolean hasRange() {
        return hasRange;
    }

    public IsochronesRequestEnums.RangeType getRangeType() {
        return rangeType;
    }

    public void setRangeType(IsochronesRequestEnums.RangeType rangeType) {
        this.rangeType = rangeType;
        hasRangeType = true;
    }

    public boolean hasRangeType() {
        return hasRangeType;
    }

    public Double getInterval() {
        return interval;
    }

    public void setInterval(Double interval) {
        this.interval = interval;
        hasInterval = true;
    }

    public boolean hasInterval() {
        return hasInterval;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
        hasTime = true;
    }

    public boolean hasTime() {
        return hasTime;
    }

    public IsochroneMapCollection getIsoMaps() {
        return isoMaps;
    }

    public void setIsoMaps(IsochroneMapCollection isoMaps) {
        this.isoMaps = isoMaps;
    }

    public IsochroneRequest getIsochroneRequest() {
        return isochroneRequest;
    }

    public void setIsochroneRequest(IsochroneRequest isochroneRequest) {
        this.isochroneRequest = isochroneRequest;
    }
}
