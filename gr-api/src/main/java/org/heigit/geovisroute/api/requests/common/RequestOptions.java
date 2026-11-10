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

package org.heigit.geovisroute.api.requests.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.requests.matrix.MatrixRequest;
import org.heigit.geovisroute.api.requests.routing.RequestProfileParams;
import org.heigit.geovisroute.api.APIEnums;
import org.heigit.geovisroute.routing.RouteRequestParameterNames;
import org.json.simple.JSONObject;

@Schema(name = "Matrix Options", description = "矩阵的高级选项", subTypes = {MatrixRequest.class})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RequestOptions implements RouteRequestParameterNames {

    @Schema(name = PARAM_AVOID_FEATURES, description = "需要避让的要素列表。 ",
            extensions = {@Extension(name = "itemRestrictions", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "itemsWhen", value = "{\"driving-*\":[\"highways\",\"tollways\",\"ferries\"],\"cycling-*\":[\"ferries\",\"steps\",\"fords\"],\"foot-*\":[\"ferries\",\"fords\",\"steps\"],\"wheelchair\":[\"ferries\",\"steps\"]}", parseValue = true)}
            )},
            example = "[\"highways\"]")
    @JsonProperty(PARAM_AVOID_FEATURES)
    private APIEnums.AvoidFeatures[] avoidFeatures;
    @JsonIgnore
    private boolean hasAvoidFeatures = false;

    @Schema(name = PARAM_AVOID_BORDERS, description = "`all` 表示不穿越任何边界。`controlled` 表示可穿越开放边界但避让受控边界。仅适用于 `driving-*` profile。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-*")}
            )},
            example = "controlled")
    @JsonProperty(PARAM_AVOID_BORDERS)
    private APIEnums.AvoidBorders avoidBorders;
    @JsonIgnore
    private boolean hasAvoidBorders = false;

    @Schema(name = PARAM_AVOID_COUNTRIES, description = """
            使用 `driving-*` profile 时需要从矩阵中排除的国家列表。可与 `'avoid_borders': 'controlled'` 一起使用。 \
            `[ 11, 193 ]` 会排除奥地利和瑞士。国家列表及应用示例可在[此处](https://giscience.github.io/openrouteservice/technical-details/country-list)查看。 \
            此外，也可以使用 ISO 标准国家代码代替数字 id，例如用 DE 或 DEU 表示德国。 \
            """,
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-*")}
            )},
            example = "[ 11, 193 ]")
    @JsonProperty(PARAM_AVOID_COUNTRIES)
    private String[] avoidCountries;
    @JsonIgnore
    private boolean hasAvoidCountries = false;

    @Schema(name = PARAM_VEHICLE_TYPE, description = "（仅适用于 profile=driving-hgv）：hgv、bus、agricultural、delivery、forestry 和 goods。车辆限制要生效必须提供该参数。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-hgv")}
            )},
            defaultValue = "hgv")
    @JsonProperty(value = PARAM_VEHICLE_TYPE)
    private APIEnums.VehicleType vehicleType;
    @JsonIgnore
    private boolean hasVehicleType = false;

    @Schema(name = PARAM_PROFILE_PARAMS, description = " 指定额外的矩阵参数。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "valueNot", value = "driving-car")}
            )})
    @JsonProperty(PARAM_PROFILE_PARAMS)
    private RequestProfileParams profileParams;
    @JsonIgnore
    private boolean hasProfileParams = false;

    @Schema(name = PARAM_AVOID_POLYGONS, description = "包含路线需要避让的区域。以 GeoJSON 的 Polygon 或 MultiPolygon 对象格式表示。")
    @JsonProperty(PARAM_AVOID_POLYGONS)
    private JSONObject avoidPolygonFeatures;
    @JsonIgnore
    private boolean hasAvoidPolygonFeatures = false;

    public APIEnums.AvoidFeatures[] getAvoidFeatures() {
        return avoidFeatures;
    }

    public void setAvoidFeatures(APIEnums.AvoidFeatures[] avoidFeatures) {
        this.avoidFeatures = avoidFeatures;
        hasAvoidFeatures = true;
    }

    public APIEnums.AvoidBorders getAvoidBorders() {
        return avoidBorders;
    }

    public void setAvoidBorders(APIEnums.AvoidBorders avoidBorders) {
        this.avoidBorders = avoidBorders;
        hasAvoidBorders = true;
    }

    public String[] getAvoidCountries() {
        return avoidCountries;
    }

    public void setAvoidCountries(String[] avoidCountries) {
        this.avoidCountries = avoidCountries;
        hasAvoidCountries = true;
    }

    public APIEnums.VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(APIEnums.VehicleType vehicleType) {
        this.vehicleType = vehicleType;
        hasVehicleType = true;
    }

    public RequestProfileParams getProfileParams() {
        return profileParams;
    }

    public void setProfileParams(RequestProfileParams profileParams) {
        this.profileParams = profileParams;
        hasProfileParams = true;
    }

    public JSONObject getAvoidPolygonFeatures() {
        return avoidPolygonFeatures;
    }

    public void setAvoidPolygonFeatures(JSONObject avoidPolygonFeatures) {
        this.avoidPolygonFeatures = avoidPolygonFeatures;
        hasAvoidPolygonFeatures = true;
    }

    public boolean hasAvoidFeatures() {
        return hasAvoidFeatures;
    }

    public boolean hasAvoidBorders() {
        return hasAvoidBorders;
    }

    public boolean hasAvoidCountries() {
        return hasAvoidCountries;
    }

    public boolean hasVehicleType() {
        return hasVehicleType;
    }

    public boolean hasProfileParams() {
        return hasProfileParams;
    }

    public boolean hasAvoidPolygonFeatures() {
        return hasAvoidPolygonFeatures;
    }
}
