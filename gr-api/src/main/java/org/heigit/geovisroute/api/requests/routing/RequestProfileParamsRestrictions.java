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
import org.heigit.geovisroute.api.APIEnums;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "Restrictions", description = "描述路线规划中应用于边的限制条件。任何不满足这些限制条件的边都不会被通行。")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RequestProfileParamsRestrictions {
    public static final String PARAM_LENGTH = "length";
    public static final String PARAM_WIDTH = "width";
    public static final String PARAM_HEIGHT = "height";
    public static final String PARAM_AXLE_LOAD = "axleload";
    public static final String PARAM_WEIGHT = "weight";
    public static final String PARAM_HAZMAT = "hazmat";
    public static final String PARAM_SURFACE_TYPE = "surface_type";
    public static final String PARAM_TRACK_TYPE = "track_type";
    public static final String PARAM_SMOOTHNESS_TYPE = "smoothness_type";
    public static final String PARAM_MAXIMUM_SLOPED_KERB = "maximum_sloped_kerb";
    public static final String PARAM_MAX_INCLINE = "maximum_incline";
    public static final String PARAM_MIN_WIDTH = "minimum_width";
    // 军车通过性参数
    public static final String PARAM_MAX_GRADIENT = "maximum_gradient";
    public static final String PARAM_MAX_FORD_DEPTH = "maximum_ford_depth";
    public static final String PARAM_MAX_BRIDGE_LOAD = "maximum_bridge_load";

    @Schema(name = PARAM_LENGTH, description = "长度限制，单位：米。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-hgv")}
            )}, example = "8.4")
    @JsonProperty(PARAM_LENGTH)
    private Float length;
    private boolean hasLength = false;

    @Schema(name = PARAM_WIDTH, description = "宽度限制，单位：米。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-hgv")}
            )}, example = "5.6")
    @JsonProperty(PARAM_WIDTH)
    private Float width;
    @JsonIgnore
    private boolean hasWidth = false;

    @Schema(name = PARAM_HEIGHT, description = "高度限制，单位：米。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-hgv")}
            )}, example = "4.2")
    @JsonProperty(PARAM_HEIGHT)
    private Float height;
    @JsonIgnore
    private boolean hasHeight = false;

    @Schema(name = PARAM_AXLE_LOAD, description = "轴荷限制，单位：吨。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-hgv")}
            )}, example = "50")
    @JsonProperty(PARAM_AXLE_LOAD)
    private Float axleLoad;
    @JsonIgnore
    private boolean hasAxleLoad = false;

    @Schema(name = PARAM_WEIGHT, description = "重量限制，单位：吨。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-hgv")}
            )}, example = "40")
    @JsonProperty(PARAM_WEIGHT)
    private Float weight;
    @JsonIgnore
    private boolean hasWeight = false;

    @Schema(name = PARAM_MAX_GRADIENT, description = "车辆最大爬坡能力，单位：百分比（%），例如 30 表示 30%。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-military")}
            )}, example = "30")
    @JsonProperty(PARAM_MAX_GRADIENT)
    private Float maxGradient;
    @JsonIgnore
    private boolean hasMaxGradient = false;

    @Schema(name = PARAM_MAX_FORD_DEPTH, description = "车辆最大涉水深度，单位：米。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-military")}
            )}, example = "1.2")
    @JsonProperty(PARAM_MAX_FORD_DEPTH)
    private Float maxFordDepth;
    @JsonIgnore
    private boolean hasMaxFordDepth = false;

    @Schema(name = PARAM_MAX_BRIDGE_LOAD, description = "车辆可通过的桥梁最大承载，单位：吨。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-military")}
            )}, example = "60")
    @JsonProperty(PARAM_MAX_BRIDGE_LOAD)
    private Float maxBridgeLoad;
    @JsonIgnore
    private boolean hasMaxBridgeLoad = false;

    @Schema(name = PARAM_HAZMAT, description = "指定是否使用适用于危险品运输的路线规划，并避开水源保护区。默认值为 `false`。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "driving-hgv")}
            )},
            defaultValue = "false")
    @JsonProperty(value = PARAM_HAZMAT)
    private boolean hazardousMaterial;
    @JsonIgnore
    private boolean hasHazardousMaterial = false;

    @Schema(name = PARAM_SURFACE_TYPE, description = "指定最低路面类型。默认值为 `sett`。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            defaultValue = "sett")
    @JsonProperty(PARAM_SURFACE_TYPE)
    private String surfaceType;
    @JsonIgnore
    private boolean hasSurfaceType = false;

    @Schema(name = PARAM_TRACK_TYPE, description = "指定道路的最低等级。默认值为 `grade1`。 ",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            defaultValue = "grade1")
    @JsonProperty(PARAM_TRACK_TYPE)
    private String trackType;
    @JsonIgnore
    private boolean hasTrackType = false;

    @Schema(name = PARAM_SMOOTHNESS_TYPE, description = "指定路线的最低平整度。默认值为 `good`。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            defaultValue = "good")
    @JsonProperty(value = PARAM_SMOOTHNESS_TYPE)
    private APIEnums.SmoothnessTypes smoothnessType;
    @JsonIgnore
    private boolean hasSmoothnessType = false;

    @Schema(name = PARAM_MAXIMUM_SLOPED_KERB, description = "指定斜路缘的最大高度，单位：米。取值为 `0.03`、`0.06`（默认）、`0.1`。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            defaultValue = "0.6")
    @JsonProperty(PARAM_MAXIMUM_SLOPED_KERB)
    private Float maxSlopedKerb;
    @JsonIgnore
    private boolean hasMaxSlopedKerb = false;

    @Schema(name = PARAM_MAX_INCLINE, description = "指定最大坡度（百分比）。`3`、`6`（默认）、`10`、`15。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            defaultValue = "6")
    @JsonProperty(PARAM_MAX_INCLINE)
    private Integer maxIncline;
    @JsonIgnore
    private boolean hasMaxIncline = false;

    @Schema(name = PARAM_MIN_WIDTH, description = "指定人行道的最小宽度，单位：米。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "profile"),
                    @ExtensionProperty(name = "value", value = "wheelchair")}
            )},
            example = "2.5")
    @JsonProperty(PARAM_MIN_WIDTH)
    private Float minWidth;
    @JsonIgnore
    private boolean hasMinWidth = false;

    public Float getLength() {
        return length;
    }

    public void setLength(Float length) {
        this.length = length;
        hasLength = true;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
        hasWidth = true;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
        hasHeight = true;
    }

    public Float getAxleLoad() {
        return axleLoad;
    }

    public void setAxleLoad(Float axleLoad) {
        this.axleLoad = axleLoad;
        hasAxleLoad = true;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
        hasWeight = true;
    }

    public Float getMaxGradient() {
        return maxGradient;
    }

    public void setMaxGradient(Float maxGradient) {
        this.maxGradient = maxGradient;
        this.hasMaxGradient = true;
    }

    public Float getMaxFordDepth() {
        return maxFordDepth;
    }

    public void setMaxFordDepth(Float maxFordDepth) {
        this.maxFordDepth = maxFordDepth;
        this.hasMaxFordDepth = true;
    }

    public Float getMaxBridgeLoad() {
        return maxBridgeLoad;
    }

    public void setMaxBridgeLoad(Float maxBridgeLoad) {
        this.maxBridgeLoad = maxBridgeLoad;
        this.hasMaxBridgeLoad = true;
    }

    public void setHazardousMaterial(Boolean hazardousMaterial) {
        this.hazardousMaterial = hazardousMaterial;
        hasHazardousMaterial = true;
    }

    public String getSurfaceType() {
        return surfaceType;
    }

    public void setSurfaceType(String surfaceType) {
        this.surfaceType = surfaceType;
        hasSurfaceType = true;
    }

    public String getTrackType() {
        return trackType;
    }

    public void setTrackType(String trackType) {
        this.trackType = trackType;
        hasTrackType = true;
    }

    public APIEnums.SmoothnessTypes getSmoothnessType() {
        return smoothnessType;
    }

    public void setSmoothnessType(APIEnums.SmoothnessTypes smoothnessType) {
        this.smoothnessType = smoothnessType;
        hasSmoothnessType = true;
    }

    public Float getMaxSlopedKerb() {
        return maxSlopedKerb;
    }

    public void setMaxSlopedKerb(Float maxSlopedKerb) {
        this.maxSlopedKerb = maxSlopedKerb;
        hasMaxSlopedKerb = true;
    }

    public Integer getMaxIncline() {
        return maxIncline;
    }

    public void setMaxIncline(Integer maxIncline) {
        this.maxIncline = maxIncline;
        hasMaxIncline = true;
    }

    public Float getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(Float minWidth) {
        this.minWidth = minWidth;
        this.hasMinWidth = true;
    }

    public boolean hasLength() {
        return hasLength;
    }

    public boolean hasWidth() {
        return hasWidth;
    }

    public boolean hasHeight() {
        return hasHeight;
    }

    public boolean hasAxleLoad() {
        return hasAxleLoad;
    }

    public boolean hasWeight() {
        return hasWeight;
    }

    public boolean hasMaxGradient() {
        return hasMaxGradient;
    }

    public boolean hasMaxFordDepth() {
        return hasMaxFordDepth;
    }

    public boolean hasMaxBridgeLoad() {
        return hasMaxBridgeLoad;
    }

    public boolean getHazardousMaterial() {
        return hazardousMaterial;
    }

    public boolean hasHazardousMaterial() {
        return hasHazardousMaterial;
    }

    public boolean hasSurfaceType() {
        return hasSurfaceType;
    }

    public boolean hasTrackType() {
        return hasTrackType;
    }

    public boolean hasSmoothnessType() {
        return hasSmoothnessType;
    }

    public boolean hasMaxSlopedKerb() {
        return hasMaxSlopedKerb;
    }

    public boolean hasMaxIncline() {
        return hasMaxIncline;
    }

    public boolean hasMinWidth() {
        return hasMinWidth;
    }

    @JsonIgnore
    public List<String> getRestrictionsThatAreSet() {
        List<String> setRestrictions = new ArrayList<>();
        if (hasLength)
            setRestrictions.add(PARAM_LENGTH);
        if (hasWidth)
            setRestrictions.add(PARAM_WIDTH);
        if (hasHeight)
            setRestrictions.add(PARAM_HEIGHT);
        if (hasAxleLoad)
            setRestrictions.add(PARAM_AXLE_LOAD);
        if (hasWeight)
            setRestrictions.add(PARAM_WEIGHT);
        if (hasHazardousMaterial)
            setRestrictions.add(PARAM_HAZMAT);
        if (hasMaxGradient)
            setRestrictions.add(PARAM_MAX_GRADIENT);
        if (hasMaxFordDepth)
            setRestrictions.add(PARAM_MAX_FORD_DEPTH);
        if (hasMaxBridgeLoad)
            setRestrictions.add(PARAM_MAX_BRIDGE_LOAD);
        if (hasSurfaceType)
            setRestrictions.add(PARAM_SURFACE_TYPE);
        if (hasTrackType)
            setRestrictions.add(PARAM_TRACK_TYPE);
        if (hasSmoothnessType)
            setRestrictions.add(PARAM_SMOOTHNESS_TYPE);
        if (hasMaxSlopedKerb)
            setRestrictions.add(PARAM_MAXIMUM_SLOPED_KERB);
        if (hasMaxIncline)
            setRestrictions.add(PARAM_MAX_INCLINE);
        if (hasMinWidth)
            setRestrictions.add(PARAM_MIN_WIDTH);
        return setRestrictions;
    }
}
