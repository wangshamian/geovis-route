package org.heigit.geovisroute.api.responses.snapping.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.config.EndpointsProperties;
import org.heigit.geovisroute.api.config.SystemMessageProperties;
import org.heigit.geovisroute.api.requests.snapping.SnappingApiRequest;
import org.heigit.geovisroute.api.responses.matrix.json.JSON2DSources;
import org.heigit.geovisroute.api.responses.matrix.json.JSONLocation;
import org.heigit.geovisroute.api.responses.snapping.SnappingResponse;
import org.heigit.geovisroute.api.responses.snapping.SnappingResponseInfo;
import org.heigit.geovisroute.matrix.ResolvedLocation;
import org.heigit.geovisroute.snapping.SnappingResult;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "SnappingResponse", description = "吸附响应包含吸附后的坐标。")
public class JsonSnappingResponse extends SnappingResponse {
    @Schema(description = "吸附后的位置，包含坐标和吸附距离。")
    @JsonProperty("locations")
    List<JSONLocation> locations;

    @JsonProperty("metadata")
    @Schema(description = "关于服务和请求的信息")
    SnappingResponseInfo responseInformation;
    public JsonSnappingResponse(SnappingResult result, SnappingApiRequest request, SystemMessageProperties systemMessageProperties, EndpointsProperties endpointsProperties) {
        super(result);
        locations = constructLocations(result);
        responseInformation = new SnappingResponseInfo(request, systemMessageProperties, endpointsProperties);
    }

    private List<JSONLocation> constructLocations(SnappingResult result) {
        List<JSONLocation> locs = new ArrayList<JSONLocation>();
        for (ResolvedLocation location: result.getLocations()) {
            if (location == null) {
                locs.add(null);
            } else {
                locs.add(new JSON2DSources(location, true));
            }
        }
        return locs;
    }
}
