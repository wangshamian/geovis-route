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
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "Alternative Routes", name = "alternativeRoutes", description = "指定是否计算备选路线，并提供确定合适备选路线的算法参数。")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RouteRequestAlternativeRoutes {
    public static final String PARAM_TARGET_COUNT = "target_count";
    public static final String PARAM_WEIGHT_FACTOR = "weight_factor";
    public static final String PARAM_SHARE_FACTOR = "share_factor";

    @Schema(name = PARAM_TARGET_COUNT, description = "要计算的备选路线目标数量。服务最多返回该数量的路线，这些路线需满足 share-factor 与 weight-factor 约束。", example = "2")
    @JsonProperty(PARAM_TARGET_COUNT)
    private Integer targetCount;
    @JsonIgnore
    private boolean hasTargetCount = false;

    @Schema(name = PARAM_WEIGHT_FACTOR, description = "路线权重相对最优路线允许偏离的最大倍数。默认值 1.4 表示备选路线的长度（代价）最多可达最优路线的 1.4 倍。", example = "1.4")
    @JsonProperty(PARAM_WEIGHT_FACTOR)
    private Double weightFactor;
    @JsonIgnore
    private boolean hasWeightFactor = false;

    @Schema(name = PARAM_SHARE_FACTOR, description = "备选路线与最优路线允许共享的最大比例。默认值 0.6 表示备选路线最多可与最优路线共享 60% 的路段。", example = "0.6")
    @JsonProperty(PARAM_SHARE_FACTOR)
    private Double shareFactor;
    @JsonIgnore
    private boolean hasShareFactor = false;

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
        hasTargetCount = true;
    }

    public boolean hasTargetCount() {
        return hasTargetCount;
    }

    public Double getWeightFactor() {
        return weightFactor;
    }

    public void setWeightFactor(Double weightFactor) {
        this.weightFactor = weightFactor;
        hasWeightFactor = true;
    }

    public boolean hasWeightFactor() {
        return hasWeightFactor;
    }

    public Double getShareFactor() {
        return shareFactor;
    }

    public void setShareFactor(Double shareFactor) {
        this.shareFactor = shareFactor;
        hasShareFactor = true;
    }

    public boolean hasShareFactor() {
        return hasShareFactor;
    }
}
