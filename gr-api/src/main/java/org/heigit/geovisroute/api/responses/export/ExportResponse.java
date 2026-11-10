package org.heigit.geovisroute.api.responses.export;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.heigit.geovisroute.api.responses.common.boundingbox.BoundingBox;
import org.heigit.geovisroute.export.ExportResult;

//TODO 重构：是否应该像 RouteResponse 那样包含 ExportResponseInfo？
public class ExportResponse {
    @JsonIgnore
    protected BoundingBox bbox;

    @JsonIgnore
    protected ExportResult exportResults;

    public ExportResponse() {
    }

    // 在 RouteResponse 中，此方法用于从 RouteRequest 获取元数据。
    public ExportResponse(ExportResult result) {
        this.exportResults = result;
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    public ExportResult getExportResults() {
        return exportResults;
    }
}
