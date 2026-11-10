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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "Profile Parameters", name = "profileParameters", description = "指定额外的路线规划参数。适用于除 `driving-car` 以外的所有 profile。", subTypes = {RequestProfileParamsRestrictions.class, RequestProfileParamsWeightings.class})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RequestProfileParams {
    public static final String PARAM_WEIGHTINGS = "weightings";
    public static final String PARAM_RESTRICTIONS = "restrictions";
    public static final String PARAM_SURFACE_QUALITY_KNOWN = "surface_quality_known";
    public static final String PARAM_ALLOW_UNSUITABLE = "allow_unsuitable";

    @JsonProperty(PARAM_WEIGHTINGS)
    private RequestProfileParamsWeightings weightings;
    @JsonIgnore
    private boolean hasWeightings = false;

    @JsonProperty(PARAM_RESTRICTIONS)
    private RequestProfileParamsRestrictions restrictions;
    @JsonIgnore
    private boolean hasRestrictions = false;

    @Schema(name = PARAM_SURFACE_QUALITY_KNOWN, description = "指定是否强制只考虑已知路面质量信息的道路——默认 false",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            example = "true")
    @JsonProperty(PARAM_SURFACE_QUALITY_KNOWN)
    private boolean surfaceQualityKnown;
    @JsonIgnore
    private boolean hasSurfaceQualityKnown = false;

    @Schema(name = PARAM_ALLOW_UNSUITABLE, description = "指定在路线查找时是否包含可能不合适的道路（例如行人用途未知的道路）——默认 false",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            example = "true")
    @JsonProperty(PARAM_ALLOW_UNSUITABLE)
    private boolean allowUnsuitable;
    @JsonIgnore
    private boolean hasAllowUnsuitable = false;

    public RequestProfileParamsWeightings getWeightings() {
        return weightings;
    }

    public void setWeightings(RequestProfileParamsWeightings weightings) {
        this.weightings = weightings;
        hasWeightings = true;
    }

    public RequestProfileParamsRestrictions getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(RequestProfileParamsRestrictions restrictions) {
        this.restrictions = restrictions;
        hasRestrictions = true;
    }

    public boolean getSurfaceQualityKnown() {
        return surfaceQualityKnown;
    }

    public void setSurfaceQualityKnown(boolean surfaceQualityKnown) {
        this.surfaceQualityKnown = surfaceQualityKnown;
        hasSurfaceQualityKnown = true;
    }

    public boolean getAllowUnsuitable() {
        return allowUnsuitable;
    }

    public void setAllowUnsuitable(boolean allowUnsuitable) {
        this.allowUnsuitable = allowUnsuitable;
        hasAllowUnsuitable = true;
    }

    public boolean hasWeightings() {
        return hasWeightings;
    }

    public boolean hasRestrictions() {
        return hasRestrictions;
    }

    public boolean hasAllowUnsuitable() {
        return hasAllowUnsuitable;
    }

    public boolean hasSurfaceQualityKnown() {
        return hasSurfaceQualityKnown;
    }
}
