package org.heigit.geovisroute.config.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义路网（GeoJSON）配置。
 */
@Getter
@Setter
public class CustomRoadNetworkProperties {
    /**
     * GeoJSON 文件路径，支持 FeatureCollection、Feature、LineString、MultiLineString。
     */
    @JsonProperty("file")
    private String file;
    /**
     * add：把 GeoJSON 作为新道路注入路网图；overlay：按几何匹配已有 OSM 边并覆盖限高限宽限重等属性。
     */
    @JsonProperty("mode")
    private String mode = "add";
    /**
     * add 模式下与已有路网节点做吸附的最大距离（米）。0 表示只按坐标去重，不做吸附。
     */
    @JsonProperty("snap_radius")
    private Double snapRadius = 25.0;
    /**
     * overlay 模式下，GeoJSON 线与已有 OSM 边的最大匹配距离（米）。
     */
    @JsonProperty("match_tolerance")
    private Double matchTolerance = 10.0;
    /**
     * overlay 模式下要覆盖的属性，GeoJSON properties 中同名的值会写入匹配到的边。
     */
    @JsonProperty("overlay_tags")
    private List<String> overlayTags = new ArrayList<>(List.of(
            "maxheight", "maxwidth", "maxweight", "maxlength", "maxaxleload"));
    /**
     * add 模式下使用的默认 OSM 标签，GeoJSON 要素自身的 properties 会覆盖同名标签。
     */
    @JsonProperty("tags")
    private Map<String, String> tags = new LinkedHashMap<>();
}
