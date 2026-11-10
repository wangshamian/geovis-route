package org.heigit.geovisroute.api.responses.snapping.geojson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.util.shapes.BBox;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.config.EndpointsProperties;
import org.heigit.geovisroute.api.config.SystemMessageProperties;
import org.heigit.geovisroute.api.requests.snapping.SnappingApiRequest;
import org.heigit.geovisroute.api.responses.common.boundingbox.BoundingBox;
import org.heigit.geovisroute.api.responses.common.boundingbox.BoundingBoxBase;
import org.heigit.geovisroute.api.responses.matrix.json.JSON2DSources;
import org.heigit.geovisroute.api.responses.routing.json.JSONBoundingBox;
import org.heigit.geovisroute.api.responses.snapping.SnappingResponse;
import org.heigit.geovisroute.api.responses.snapping.SnappingResponseInfo;
import org.heigit.geovisroute.matrix.ResolvedLocation;
import org.heigit.geovisroute.snapping.SnappingResult;
import org.heigit.geovisroute.util.GeomUtility;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "GeoJSONSnappingResponse", description = "GeoJSON 吸附响应以 GeoJSON 格式包含吸附后的坐标。")
public class GeoJSONSnappingResponse extends SnappingResponse {

    @JsonIgnore
    protected BoundingBox bbox;

    @JsonProperty("type")
    @Schema(description = "GeoJSON 类型", defaultValue = "FeatureCollection")
    public final String type = "FeatureCollection";

    @JsonProperty("bbox")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "覆盖所有返回吸附点的边界框", example = "[49.414057, 8.680894, 49.420514, 8.690123]")
    public double[] getBBoxAsArray() {
        return bbox.getAsArray();
    }

    @JsonProperty("features")
    @Schema(description = "关于各要素的信息")
    public List<GeoJSONFeature> features;

    @JsonProperty("metadata")
    @Schema(description = "关于服务和请求的信息")
    SnappingResponseInfo responseInformation;

    public GeoJSONSnappingResponse(SnappingResult result, SnappingApiRequest request, SystemMessageProperties systemMessageProperties, EndpointsProperties endpointsProperties) {
        super(result);
        this.features = new ArrayList<>();
        List<BBox> bBoxes = new ArrayList<>();
        for (int sourceId = 0; sourceId < result.getLocations().length; sourceId++){
            ResolvedLocation resolvedLocation = result.getLocations()[sourceId];
            if (resolvedLocation != null) {
                // 为每个点创建 BBox，以使用现有的 generateBoundingFromMultiple 函数
                double x = resolvedLocation.getCoordinate().x;
                double y = resolvedLocation.getCoordinate().y;
                bBoxes.add(new BBox(x,x,y,y));
                this.features.add(new GeoJSONFeature(sourceId, new JSON2DSources(resolvedLocation, true)));
            }
        }

        BBox[] boxes = bBoxes.toArray(new BBox[0]);
        if (boxes.length > 0) {
            this.bbox = new BoundingBoxBase(GeomUtility.generateBoundingFromMultiple(boxes));
        } else {
            this.bbox = new JSONBoundingBox(new BBox(0,0,0,0));
        }

        responseInformation = new SnappingResponseInfo(request, systemMessageProperties, endpointsProperties);
    }
}
