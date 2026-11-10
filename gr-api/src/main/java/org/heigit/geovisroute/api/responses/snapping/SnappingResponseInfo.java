package org.heigit.geovisroute.api.responses.snapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.util.Helper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.config.EndpointsProperties;
import org.heigit.geovisroute.api.config.SystemMessageProperties;
import org.heigit.geovisroute.api.requests.snapping.SnappingApiRequest;
import org.heigit.geovisroute.api.responses.common.engineinfo.EngineInfo;
import org.heigit.geovisroute.util.AppInfo;
import org.heigit.geovisroute.api.util.SystemMessage;

@Schema(description = "关于请求的信息")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SnappingResponseInfo {
    @Schema(description = "版权与署名信息", example = "openrouteservice.org | OpenStreetMap contributors")
    @JsonProperty("attribution")
    private String attribution;
    @Schema(description = "所请求的服务", example = "snap")
    @JsonProperty("service")
    private final String service;
    @Schema(description = "发出请求的时间（UNIX Epoch 时间）", example = "1549549847974")
    @JsonProperty("timestamp")
    private final long timeStamp;

    @Schema(description = "用于生成该请求所使用的信息")
    @JsonProperty("query")
    private final SnappingApiRequest request;

    @Schema(description = "关于吸附服务的信息")
    @JsonProperty("engine")
    private final EngineInfo engineInfo;

    @Schema(description = "系统消息", example = "A message string configured in the service")
    @JsonProperty("system_message")
    private final String systemMessage;

    public SnappingResponseInfo(SnappingApiRequest request, SystemMessageProperties systemMessageProperties, EndpointsProperties endpointsProperties) {
        service = "snap";
        timeStamp = System.currentTimeMillis();

        if (!Helper.isEmpty(endpointsProperties.getSnap().getAttribution()))
            attribution = endpointsProperties.getSnap().getAttribution();

        engineInfo = new EngineInfo(AppInfo.getEngineInfo());

        this.request = request;

        this.systemMessage = SystemMessage.getSystemMessage(request, systemMessageProperties);
    }
}
