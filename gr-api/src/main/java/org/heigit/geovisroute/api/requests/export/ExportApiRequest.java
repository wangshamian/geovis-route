package org.heigit.geovisroute.api.requests.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.APIEnums;
import org.heigit.geovisroute.api.requests.common.APIRequest;

import java.util.List;

@Schema(title = "Graph export Service", name = "graphExportService", description = "路网图导出服务端点。")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ExportApiRequest extends APIRequest {
    public static final String PARAM_ID = "id";
    public static final String PARAM_BBOX = "bbox";
    public static final String PARAM_PROFILE = "profile";
    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_GEOMETRY = "geometry";
    public static final String PARAM_ADDITIONAL_INFO = "additional_info";

    @Schema(name = PARAM_ID, description = "请求的任意标识字符串，会反映在 meta 信息中。",
            example = "export_request")
    @JsonProperty(PARAM_ID)
    private String id;
    @JsonIgnore
    private boolean hasId = false;

    @Schema(name = PARAM_PROFILE, hidden = true)
    private APIEnums.Profile profile;

    @Schema(name = PARAM_BBOX, description = "请求使用的边界框，以 `longitude/latitude` 对组成的数组表示",
            example = "[[8.681495,49.41461],[8.686507,49.41943]]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(PARAM_BBOX)
    private List<List<Double>> bbox; //显然，这里必须使用非基本类型…

    @Schema(name = PARAM_FORMAT, hidden = true)
    @JsonProperty(PARAM_FORMAT)
    private APIEnums.ExportResponseType responseType = APIEnums.ExportResponseType.JSON;

    @Schema(name = PARAM_GEOMETRY, description = "是否返回路网图的实际几何，或返回简化（直线）表示。", example = "true", defaultValue = "true")
    @JsonProperty(PARAM_GEOMETRY)
    private boolean geometry = true;

    @Schema(name = PARAM_ADDITIONAL_INFO, hidden = true)
    @JsonProperty(PARAM_ADDITIONAL_INFO)
    private boolean additionalInfo;

    @JsonCreator
    public ExportApiRequest(@JsonProperty(value = PARAM_BBOX, required = true) List<List<Double>> bbox) {
        this.bbox = bbox;
    }

    public boolean hasId() {
        return hasId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.hasId = true;
    }

    public boolean additionalInfo() {
        return additionalInfo;
    }

    public List<List<Double>> getBbox() {
        return bbox;
    }

    public void setBbox(List<List<Double>> bbox) {
        this.bbox = bbox;
    }

    public APIEnums.Profile getProfile() {
        return profile;
    }

    public void setProfile(APIEnums.Profile profile) {
        this.profile = profile;
    }

    public void setResponseType(APIEnums.ExportResponseType responseType) {
        this.responseType = responseType;
    }

    public APIEnums.ExportResponseType getResponseType() {
        return responseType;
    }

    public boolean getGeometry() {
        return geometry;
    }
}
