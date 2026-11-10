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

package org.heigit.geovisroute.api.responses.routing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.util.Helper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.config.EndpointsProperties;
import org.heigit.geovisroute.api.config.SystemMessageProperties;
import org.heigit.geovisroute.api.requests.routing.RouteRequest;
import org.heigit.geovisroute.api.responses.common.engineinfo.EngineInfo;
import org.heigit.geovisroute.util.AppInfo;
import org.heigit.geovisroute.api.util.SystemMessage;

@Schema(description = "关于请求的信息")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RouteResponseInfo {
    @Schema(description = "请求的 ID（由查询传入）", example = "request123")
    @JsonProperty("id")
    private String id;

    @Schema(description = "版权与署名信息", example = "openrouteservice.org | OpenStreetMap contributors")
    @JsonProperty("attribution")
    private String attribution;
    @Schema(description = "所请求的服务", example = "routing")
    @JsonProperty("service")
    private final String service;
    @Schema(description = "发出请求的时间（UNIX Epoch 时间）", example = "1549549847974")
    @JsonProperty("timestamp")
    private final long timeStamp;

    @Schema(description = "用于生成该路线所使用的信息")
    @JsonProperty("query")
    private final RouteRequest request;

    @Schema(description = "关于路线服务的信息")
    @JsonProperty("engine")
    private final EngineInfo engineInfo;

    @Schema(description = "系统消息", example = "A message string configured in the service")
    @JsonProperty("system_message")
    private final String systemMessage;

    public RouteResponseInfo(RouteRequest request, SystemMessageProperties systemMessageProperties, EndpointsProperties endpointsProperties) {
        service = "routing";
        timeStamp = System.currentTimeMillis();

        if (!Helper.isEmpty(endpointsProperties.getRouting().getAttribution()))
            attribution = endpointsProperties.getRouting().getAttribution();

        if (request.hasId())
            id = request.getId();

        engineInfo = new EngineInfo(AppInfo.getEngineInfo());

        this.request = request;

        this.systemMessage = SystemMessage.getSystemMessage(request, systemMessageProperties);
    }
}
