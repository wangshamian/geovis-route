package org.heigit.geovisroute.api.responses.isochrones.geojson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.common.AttributeValue;
import org.heigit.geovisroute.isochrones.Isochrone;
import org.locationtech.jts.geom.Coordinate;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeoJSONIsochroneProperties {
    @Schema(description = "等时圈的 ID，依据其在 `locations` 查询参数中的位置确定。每个位置对应各自的一组多边形。", example = "1")
    @JsonProperty(value = "group_index")
    private final Integer groupIndex;

    @Schema(description = "该等时圈/等距圈的范围值，单位为秒/米。", example = "90")
    @JsonProperty("value")
    private final Double value;

    @Schema(description = "该具体分析位置的坐标。", example = "[8.676441,49.411648]")
    @JsonProperty("center")
    private final Double[] center;

    @Schema(description = "多边形的面积，单位为平方米（用于 attributes=area）。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "attributes"),
                    @ExtensionProperty(name = "containsValue", value = "area")}
            )},
            example = "567.827")
    @JsonProperty("area")
    private Double area;

    @Schema(description = """
            返回 0 到 1 之间的可达性评分（用于 attributes=reachfactor）。 \
            由于最大可达系数相当于在真空中无障碍地以最高速度直线飞行， \
            因此理论上永远不可能达到 1。不过，高速公路的存在会使评分高于普通道路。\
            """,
            example = "0.56")
    @JsonProperty("reachfactor")
    private Double reachfactor;

    @Schema(description = "多边形的总人口（用于 attributes=total_pop）。",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "attributes"),
                    @ExtensionProperty(name = "containsValue", value = "total_pop")}
            )},
            example = "562789")
    @JsonProperty(value = "total_pop")
    private Double totalPop;


    public GeoJSONIsochroneProperties(Isochrone isochrone, Coordinate center, int groupIndex) {
        this.groupIndex = groupIndex;
        if (isochrone.hasArea())
            this.area = isochrone.getArea();
        this.value = isochrone.getValue();
        this.center = new Double[]{center.x, center.y};
        if (isochrone.hasReachfactor())
            this.reachfactor = isochrone.getReachfactor();
        if (isochrone.getAttributes() != null && !isochrone.getAttributes().isEmpty())
            for (AttributeValue attributeValue : isochrone.getAttributes()) {
                if (attributeValue.getName().equalsIgnoreCase("total_pop"))
                    this.totalPop = attributeValue.getValue();
            }
    }

    public int getGroupIndex() {
        return groupIndex;
    }

    public Double getValue() {
        return value;
    }

    public Double[] getCenter() {
        return center;
    }

    public Double getArea() {
        return area;
    }

    public Double getReachfactor() {
        return reachfactor;
    }

    public Double getTotalPop() {
        return totalPop;
    }
}
