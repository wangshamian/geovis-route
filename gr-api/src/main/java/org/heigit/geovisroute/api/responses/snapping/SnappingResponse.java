package org.heigit.geovisroute.api.responses.snapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.heigit.geovisroute.snapping.SnappingResult;
public class SnappingResponse {
    @JsonIgnore
    private final SnappingResult result;

    public SnappingResponse(SnappingResult result) {
        this.result = result;
    }
}
