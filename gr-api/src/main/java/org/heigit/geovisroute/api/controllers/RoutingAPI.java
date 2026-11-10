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

package org.heigit.geovisroute.api.controllers;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.heigit.geovisroute.api.APIEnums;
import org.heigit.geovisroute.api.config.EndpointsProperties;
import org.heigit.geovisroute.api.config.SystemMessageProperties;
import org.heigit.geovisroute.api.errors.CommonResponseEntityExceptionHandler;
import org.heigit.geovisroute.api.requests.routing.RouteRequest;
import org.heigit.geovisroute.api.responses.routing.geojson.GeoJSONRouteResponse;
import org.heigit.geovisroute.api.responses.routing.gpx.GPXRouteResponse;
import org.heigit.geovisroute.api.responses.routing.json.JSONRouteResponse;
import org.heigit.geovisroute.api.services.RoutingService;
import org.heigit.geovisroute.common.EncoderNameEnum;
import org.heigit.geovisroute.exceptions.*;
import org.heigit.geovisroute.routing.RouteResult;
import org.heigit.geovisroute.routing.RoutingErrorCodes;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Directions Service", description = "获取不同交通方式的路线导航")
@RequestMapping("/v2/directions")
@ApiResponse(responseCode = "400", description = "请求不正确，因此无法处理。")
@ApiResponse(responseCode = "404", description = "无法找到某个元素。如有可能，会提供更详细的错误码。")
@ApiResponse(responseCode = "405", description = "不支持指定的 HTTP 方法。更多细节请参阅端点文档。")
@ApiResponse(responseCode = "413", description = "请求体超过服务器可处理的大小，请求中提供的数据超出了容量限制。")
@ApiResponse(responseCode = "500", description = "遇到意外错误，会提供更详细的错误码。")
@ApiResponse(responseCode = "501", description = "表示服务器不支持满足该请求所需的功能。")
@ApiResponse(responseCode = "503", description = "服务器当前因过载或维护而不可用。")
public class RoutingAPI {
    static final CommonResponseEntityExceptionHandler errorHandler = new CommonResponseEntityExceptionHandler(RoutingErrorCodes.BASE);

    private final EndpointsProperties endpointsProperties;
    private final SystemMessageProperties systemMessageProperties;
    private final RoutingService routingService;

    public RoutingAPI(EndpointsProperties endpointsProperties, SystemMessageProperties systemMessageProperties, RoutingService routingService) {
        this.endpointsProperties = endpointsProperties;
        this.systemMessageProperties = systemMessageProperties;
        this.routingService = routingService;
    }

    // 通用捕获方法 - 当 URL 中提供额外信息时，会访问其他方法。
    @GetMapping
    @Operation(hidden = true)
    public void getGetMapping() throws MissingParameterException {
        throw new MissingParameterException(RoutingErrorCodes.MISSING_PARAMETER, "profile");
    }

    @PostMapping
    @Operation(hidden = true)
    public String getPostMapping(@RequestBody RouteRequest request) throws MissingParameterException {
        throw new MissingParameterException(RoutingErrorCodes.MISSING_PARAMETER, "profile");
    }

    // 匹配任何未定义的响应类型
    @PostMapping(value = "/{profile}/*")
    @Operation(hidden = true)
    public void getInvalidResponseType() throws StatusCodeException {
        throw new StatusCodeException(HttpServletResponse.SC_NOT_ACCEPTABLE, RoutingErrorCodes.UNSUPPORTED_EXPORT_FORMAT, "This response format is not supported");
    }

    // 功能请求方法

    @GetMapping(value = "/{profile}", produces = {"application/geo+json;charset=UTF-8"})
    @Operation(
            method = "GET",
            description = """
                    使用指定 profile 获取两点之间的基础路线。响应采用 GeoJSON 格式。\
                    除 profile、起点坐标和终点坐标外，该方法不接受任何请求体或其它参数。\
                    """, summary = "Directions Service")
    @ApiResponse(
            responseCode = "200",
            description = "请求成功处理的标准响应。返回 GeoJSON。附加信息的解码值可在[此处](https://giscience.github.io/openrouteservice/api-reference/endpoints/directions/extra-info/)查看。",
            content = {@Content(
                    mediaType = "application/geo+json",
                    schema = @Schema(implementation = GeoJSONRouteResponse.class)
            )
            })
    public GeoJSONRouteResponse getSimpleGeoJsonRoute(@Parameter(description = "指定路线 profile。", required = true, example = "driving-car") @PathVariable String profile,
                                                      @Parameter(description = "路线的起点坐标，格式为 `longitude,latitude`。", required = true, example = "8.681495,49.41461") @RequestParam Coordinate start,
                                                      @Parameter(description = "路线的终点坐标，格式为 `longitude,latitude`。", required = true, example = "8.687872,49.420318") @RequestParam Coordinate end) throws StatusCodeException {
        RouteRequest request = new RouteRequest(start, end);
        request.setProfile(getProfileEnum(profile));
        request.setProfileName(profile);

        RouteResult[] result = routingService.generateRouteFromRequest(request);

        return new GeoJSONRouteResponse(result, request, systemMessageProperties, endpointsProperties);
    }

    @PostMapping(value = "/{profile}")
    @Operation(
            description = "以 JSON 形式返回指定 profile 及其设置下两个或多个位置之间的路线",
            summary = "Directions Service"
    )
    @ApiResponse(
            responseCode = "200",
            description = "请求成功处理的标准响应。返回 JSON。附加信息的解码值可在[此处](https://giscience.github.io/openrouteservice/api-reference/endpoints/directions/extra-info/)查看。",
            content = {@Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = JSONRouteResponse.class)
            )
            })
    public JSONRouteResponse getDefault(@Parameter(description = "指定路线 profile。", required = true, example = "driving-car") @PathVariable String profile,
                                        @Parameter(description = "请求负载", required = true) @RequestBody RouteRequest request) throws StatusCodeException {
        return getJsonRoute(profile, request);
    }

    @PostMapping(value = "/{profile}/json", produces = {"application/json;charset=UTF-8"})
    @Operation(
            description = "以 JSON 形式返回指定 profile 及其设置下两个或多个位置之间的路线",
            summary = "Directions Service JSON"
    )
    @ApiResponse(responseCode = "200",
            description = "JSON 响应",
            content = {@Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = JSONRouteResponse.class)
            )
            })
    public JSONRouteResponse getJsonRoute(
            @Parameter(description = "指定路线 profile。", required = true, example = "driving-car") @PathVariable String profile,
            @Parameter(description = "请求负载", required = true) @RequestBody RouteRequest request) throws StatusCodeException {
        request.setProfile(getProfileEnum(profile));
        request.setProfileName(profile);
        request.setResponseType(APIEnums.RouteResponseType.JSON);

        RouteResult[] result = routingService.generateRouteFromRequest(request);

        return new JSONRouteResponse(result, request, systemMessageProperties, endpointsProperties);
    }

    @PostMapping(value = "/{profile}/gpx", produces = "application/gpx+xml;charset=UTF-8")
    @Operation(
            description = "以 GPX 形式返回指定 profile 及其设置下两个或多个位置之间的路线。schema 可在[此处](https://raw.githubusercontent.com/GIScience/openrouteservice-schema/main/gpx/v1/ors-gpx.xsd)查看",
            summary = "Directions Service GPX"
    )
    @ApiResponse(
            responseCode = "200",
            description = "请求成功处理的标准响应。返回 GPX。",
            content = {@Content(
                    mediaType = "application/gpx+xml",
                    schema = @Schema(implementation = GPXRouteResponse.class)
            )
            })
    public GPXRouteResponse getGPXRoute(
            @Parameter(description = "指定路线 profile。", required = true, example = "driving-car") @PathVariable String profile,
            @Parameter(description = "请求负载", required = true) @RequestBody RouteRequest request) throws StatusCodeException {
        request.setProfile(getProfileEnum(profile));
        request.setProfileName(profile);
        request.setResponseType(APIEnums.RouteResponseType.GPX);

        RouteResult[] result = routingService.generateRouteFromRequest(request);

        return new GPXRouteResponse(result, request, systemMessageProperties, endpointsProperties);

    }

    @PostMapping(value = "/{profile}/geojson", produces = "application/geo+json;charset=UTF-8")
    @Operation(
            description = "以 GeoJSON 形式返回指定 profile 及其设置下两个或多个位置之间的路线",
            summary = "Directions Service GeoJSON"
    )
    @ApiResponse(
            responseCode = "200",
            description = "请求成功处理的标准响应。返回 GeoJSON。附加信息的解码值可在[此处](https://giscience.github.io/openrouteservice/api-reference/endpoints/directions/extra-info/)查看。",
            content = {@Content(
                    mediaType = "application/geo+json",
                    schema = @Schema(implementation = GeoJSONRouteResponse.class)
            )
            })
    public GeoJSONRouteResponse getGeoJsonRoute(
            @Parameter(description = "指定路线 profile。", required = true, example = "driving-car") @PathVariable String profile,
            @Parameter(description = "请求负载", required = true) @RequestBody RouteRequest request) throws StatusCodeException {
        request.setProfile(getProfileEnum(profile));
        request.setProfileName(profile);
        request.setResponseType(APIEnums.RouteResponseType.GEOJSON);

        RouteResult[] result = routingService.generateRouteFromRequest(request);

        return new GeoJSONRouteResponse(result, request, systemMessageProperties, endpointsProperties);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(final MissingServletRequestParameterException e) {
        return errorHandler.handleStatusCodeException(new MissingParameterException(RoutingErrorCodes.MISSING_PARAMETER, e.getParameterName()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConversionFailedException.class, HttpMessageConversionException.class, Exception.class})
    public ResponseEntity<Object> handleReadingBodyException(final Exception e) {
        final Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException exception) {
            return errorHandler.handleUnknownParameterException(new UnknownParameterException(RoutingErrorCodes.UNKNOWN_PARAMETER, exception.getPropertyName()));
        } else if (cause instanceof InvalidFormatException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_FORMAT, "" + exception.getValue()));
        } else if (cause instanceof ConversionFailedException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_VALUE, "" + exception.getValue()));
        } else if (cause instanceof InvalidDefinitionException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_VALUE, exception.getPath().get(0).getFieldName()));
        } else if (cause instanceof MismatchedInputException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(RoutingErrorCodes.INVALID_PARAMETER_FORMAT, exception.getPath().get(0).getFieldName()));
        } else {
            // 检查是否整体缺少请求体
            if (e.getLocalizedMessage() != null && e.getLocalizedMessage().startsWith("Required request body is missing")) {
                return errorHandler.handleStatusCodeException(new EmptyElementException(RoutingErrorCodes.MISSING_PARAMETER, "Request body could not be read"));
            }
            return errorHandler.handleGenericException(e);
        }
    }

    @ExceptionHandler(StatusCodeException.class)
    public ResponseEntity<Object> handleException(final StatusCodeException e) {
        return errorHandler.handleStatusCodeException(e);
    }

    private APIEnums.Profile getProfileEnum(String profile) throws ParameterValueException {
        EncoderNameEnum encoderForProfile = routingService.getEncoderForProfile(profile);
        return APIEnums.Profile.forValue(encoderForProfile.getEncoderName());
    }
}
