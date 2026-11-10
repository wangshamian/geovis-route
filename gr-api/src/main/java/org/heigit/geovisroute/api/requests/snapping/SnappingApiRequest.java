package org.heigit.geovisroute.api.requests.snapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.requests.common.APIRequest;
import org.heigit.geovisroute.api.APIEnums;

import java.util.List;

@Schema(name = "SnappingRequest", description = "吸附服务端点。")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SnappingApiRequest extends APIRequest {
    public static final String PARAM_PROFILE = "profile";
    public static final String PARAM_LOCATIONS = "locations";
    public static final String PARAM_MAXIMUM_SEARCH_RADIUS = "radius";
    public static final String PARAM_FORMAT = "format";

    @Schema(name = PARAM_PROFILE, hidden = true)
    private APIEnums.Profile profile;

    @Schema(name = PARAM_LOCATIONS, description = "需要吸附的位置，以 `longitude/latitude` 对数组表示。",
            example = "[[8.669629,49.413025],[8.675841,49.418532],[8.665144,49.415594]]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(PARAM_LOCATIONS)
    private List<List<Double>> locations; //显然，这里必须使用非基本类型…

    @Schema(name = PARAM_FORMAT, hidden = true)
    @JsonProperty(PARAM_FORMAT)
    private APIEnums.SnappingResponseType responseType = APIEnums.SnappingResponseType.JSON;

    @Schema(name = PARAM_MAXIMUM_SEARCH_RADIUS, description = "在给定坐标周围搜索路网边的最大半径（单位：米）。",
        example ="350", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(PARAM_MAXIMUM_SEARCH_RADIUS)
    private double maximumSearchRadius = 350;//TODO：允许在配置文件中设置特定于配置方案的值，见 #1798

    @JsonCreator
    public SnappingApiRequest(@JsonProperty(value = PARAM_LOCATIONS, required = true) List<List<Double>> locations) {
        this.locations = locations;
    }

    public APIEnums.Profile getProfile() {
        return profile;
    }

    public void setProfile(APIEnums.Profile profile) {
        this.profile = profile;
    }

    public double getMaximumSearchRadius() {
        return maximumSearchRadius;
    }

    public List<List<Double>> getLocations() {
        return locations;
    }

    public void setLocations(List<List<Double>> locations) {
        this.locations = locations;
    }

    public void setResponseType(APIEnums.SnappingResponseType responseType) {
        this.responseType = responseType;
    }
}
