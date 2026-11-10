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

package org.heigit.geovisroute.api.responses.routing.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.requests.routing.RouteRequest;
import org.heigit.geovisroute.api.responses.common.boundingbox.BoundingBoxFactory;
import org.heigit.geovisroute.common.DistanceUnit;
import org.heigit.geovisroute.exceptions.StatusCodeException;
import org.heigit.geovisroute.routing.RouteExtraInfo;
import org.heigit.geovisroute.routing.RouteResult;
import org.heigit.geovisroute.routing.RouteWarning;
import org.heigit.geovisroute.util.DistanceUnitUtil;
import org.heigit.geovisroute.util.PolylineEncoder;
import org.locationtech.jts.geom.Coordinate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(name = "JSONIndividualRouteResponse", description = "由服务创建的单个基于 JSON 的路线")
public class JSONIndividualRouteResponse extends JSONBasedIndividualRouteResponse {
    @Schema(description = "路线的几何信息。对于 JSON 路线响应，这是一个编码后的折线", example = "yuqlH{i~s@gaUe@VgEQFcBRbB_C")
    @JsonProperty("geometry")
    @JsonUnwrapped
    private final String geomResponse;

    @Schema(description = "路线的摘要信息")
    private final JSONSummary summary;

    @Schema(description = "包含组成路线的各分段及其对应步骤的列表。")
    private final List<JSONSegment> segments;

    @JsonProperty("way_points")
    @Schema(description = "包含与 *geometry* 对应的途经点索引的列表。", example = "[0,23]")
    private final List<Integer> wayPoints;

    @JsonProperty("warnings")
    @Schema(description = "为该路线生成的警告列表")
    private List<JSONWarning> warnings;

    @Schema(description = "包含组成该路线的各行程段（leg）的列表。")
    @JsonProperty("legs")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<JSONLeg> legs;

    private final Map<String, JSONExtra> extras;

    @Schema(description = "出发日期和时间",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "departure"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )}, example = "2020-01-31T12:45:00+01:00")
    @JsonProperty(value = "departure")
    protected ZonedDateTime departure;
    @Schema(description = "到达日期和时间",
            extensions = {@Extension(name = "validWhen", properties = {
                    @ExtensionProperty(name = "ref", value = "arrival"),
                    @ExtensionProperty(name = "value", value = "true", parseValue = true)}
            )}, example = "2020-01-31T13:15:00+01:00")
    @JsonProperty(value = "arrival")
    protected ZonedDateTime arrival;

    public JSONIndividualRouteResponse(RouteResult routeResult, RouteRequest request) throws StatusCodeException {
        super(routeResult, request);

        geomResponse = constructEncodedGeometry(this.routeCoordinates);

        if (this.includeElevation)
            summary = new JSONSummary(routeResult.getSummary().getDistance(), routeResult.getSummary().getDuration(), routeResult.getSummary().getAscent(), routeResult.getSummary().getDescent());
        else
            summary = new JSONSummary(routeResult.getSummary().getDistance(), routeResult.getSummary().getDuration());

        if (this.isPtRequest) {
            summary.setTransfers(routeResult.getSummary().getTransfers());
            summary.setFare(routeResult.getSummary().getFare());
        }

        if (routeResult.hasDepartureAndArrival()) {
            departure = routeResult.getDeparture();
            arrival = routeResult.getArrival();
        }

        segments = constructSegments(routeResult, request);

        bbox = BoundingBoxFactory.constructBoundingBox(routeResult.getSummary().getBBox(), request);

        wayPoints = routeResult.getWayPointsIndices();

        extras = new HashMap<>();
        List<RouteExtraInfo> responseExtras = routeResult.getExtraInfo();
        if (responseExtras != null) {
            double routeLength = routeResult.getSummary().getDistance();
            DistanceUnit units = DistanceUnit.METERS;
            if (request.hasUnits())
                units = DistanceUnitUtil.getFromString(request.getUnits().toString(), DistanceUnit.UNKNOWN);
            for (RouteExtraInfo extraInfo : responseExtras) {
                extras.put(extraInfo.getName(), new JSONExtra(extraInfo.getSegments(), extraInfo.getSummary(units, routeLength, true)));
            }
        }

        if (routeResult.getWarnings() != null && !routeResult.getWarnings().isEmpty()) {
            warnings = new ArrayList<>();
            for (RouteWarning warning : routeResult.getWarnings()) {
                warnings.add(new JSONWarning(warning));
            }
        }

        legs = constructLegs(routeResult);
    }

    private String constructEncodedGeometry(final Coordinate[] coordinates) {
        if (coordinates != null)
            return PolylineEncoder.encode(coordinates, includeElevation, new StringBuilder());
        else
            return "";
    }

    @Schema(description = "包含整条路线的边界框", example = "[49.414057, 8.680894, 49.420514, 8.690123]")
    @JsonProperty("bbox")
    public double[] getBbox() {
        return bbox.getAsArray();
    }

    public String getGeomResponse() {
        return geomResponse;
    }

    @Schema(description = "表示该路线所请求的附加信息项的附加信息对象列表。")
    @JsonProperty("extras")
    public Map<String, JSONExtra> getExtras() {
        return extras;
    }

    public JSONSummary getSummary() {
        return summary;
    }

    public List<JSONSegment> getSegments() {
        return segments;
    }

    public List<Integer> getWayPoints() {
        return wayPoints;
    }
}
