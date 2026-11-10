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
import org.heigit.geovisroute.api.errors.CommonResponseEntityExceptionHandler;
import org.heigit.geovisroute.api.requests.export.ExportApiRequest;
import org.heigit.geovisroute.api.responses.export.json.JsonExportResponse;
import org.heigit.geovisroute.api.responses.export.topojson.TopoJsonExportResponse;
import org.heigit.geovisroute.api.services.ExportService;
import org.heigit.geovisroute.common.EncoderNameEnum;
import org.heigit.geovisroute.exceptions.*;
import org.heigit.geovisroute.export.ExportErrorCodes;
import org.heigit.geovisroute.export.ExportResult;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Export Service", description = "导出不同交通方式的基础路网图")
@RequestMapping("/v2/export")
@ApiResponse(responseCode = "400", description = "请求不正确，因此无法处理。")
@ApiResponse(responseCode = "404", description = "无法找到某个元素。如有可能，会提供更详细的错误码。")
@ApiResponse(responseCode = "405", description = "不支持指定的 HTTP 方法。更多细节请参阅端点文档。")
@ApiResponse(responseCode = "413", description = "请求体超过服务器可处理的大小，请求中提供的数据超出了容量限制。")
@ApiResponse(responseCode = "500", description = "遇到意外错误，会提供更详细的错误码。")
@ApiResponse(responseCode = "501", description = "表示服务器不支持满足该请求所需的功能。")
@ApiResponse(responseCode = "503", description = "服务器当前因过载或维护而不可用。")
public class ExportAPI {
    static final CommonResponseEntityExceptionHandler errorHandler = new CommonResponseEntityExceptionHandler(ExportErrorCodes.BASE);

    private final ExportService exportService;

    public ExportAPI(ExportService exportService) {
        this.exportService = exportService;
    }

    // 通用捕获方法 - 当 URL 中提供额外信息时，会访问其他方法。
    @GetMapping
    @Operation(hidden = true)
    public void getGetMapping() throws MissingParameterException {
        throw new MissingParameterException(ExportErrorCodes.MISSING_PARAMETER, "profile");
    }

    @PostMapping
    @Operation(hidden = true)
    public String getPostMapping(@RequestBody ExportApiRequest request) throws MissingParameterException {
        throw new MissingParameterException(ExportErrorCodes.MISSING_PARAMETER, "profile");
    }

    // 功能请求方法
    @PostMapping(value = "/{profile}")
    @Operation(
            description = """
                    以 JSON 形式返回指定 profile 在给定边界框内的点、边和权重列表。\
                    除 profile、起点坐标和终点坐标外，该方法不接受任何请求体或其它参数。\
                    """,
            summary = "Export Service"
    )
    @ApiResponse(
            responseCode = "200",
            description = "请求成功处理的标准响应。返回 JSON。",
            content = {@Content(
                    mediaType = "application/geo+json",
                    schema = @Schema(implementation = JsonExportResponse.class)
            )
            })
    public JsonExportResponse getDefault(@Parameter(description = "指定路线 profile。", required = true, example = "driving-car") @PathVariable String profile,
                                         @Parameter(description = "请求负载", required = true) @RequestBody ExportApiRequest request) throws StatusCodeException {
        return getJsonExport(profile, request);
    }

    @PostMapping(value = "/{profile}/json", produces = {"application/json;charset=UTF-8"})
    @Operation(
            description = "以 JSON 形式返回指定 profile 在给定边界框内的点、边和权重列表。",
            summary = "Export Service JSON"
    )
    @ApiResponse(
            responseCode = "200",
            description = "JSON 响应。",
            content = {@Content(
                    mediaType = "application/geo+json",
                    schema = @Schema(implementation = JsonExportResponse.class)
            )
            })
    public JsonExportResponse getJsonExport(
            @Parameter(description = "指定 profile。", required = true, example = "driving-car") @PathVariable String profile,
            @Parameter(description = "请求负载", required = true) @RequestBody ExportApiRequest request) throws StatusCodeException {
        request.setProfile(getProfileEnum(profile));
        request.setProfileName(profile);
        request.setResponseType(APIEnums.ExportResponseType.JSON);

        ExportResult result = exportService.generateExportFromRequest(request);

        return new JsonExportResponse(result);
    }

    @PostMapping(value = "/{profile}/topojson", produces = {"application/json;charset=UTF-8"})
    @Operation(
            description = "返回指定 profile 在给定边界框内的边、边属性及其拓扑关系。",
            summary = "Export Service TopoJSON"
    )
    @ApiResponse(
            responseCode = "200",
            description = "TopoJSON 响应。",
            content = {@Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TopoJsonExportResponse.class)
            )
            })
    public TopoJsonExportResponse getTopoJsonExport(
            @Parameter(description = "指定 profile。", required = true, example = "driving-car") @PathVariable String profile,
            @Parameter(description = "请求负载", required = true) @RequestBody ExportApiRequest request) throws StatusCodeException {
        request.setProfile(getProfileEnum(profile));
        request.setProfileName(profile);
        request.setResponseType(APIEnums.ExportResponseType.TOPOJSON);

        ExportResult result = exportService.generateExportFromRequest(request);

        return TopoJsonExportResponse.fromExportResult(result);
    }

    // 匹配任何未定义的响应类型
    @PostMapping(value = "/{profile}/{responseType}")
    @Operation(hidden = true)
    public void getInvalidResponseType(@PathVariable String profile, @PathVariable String responseType) throws StatusCodeException {
        if (responseType.equals("topojson")) {
            throw new StatusCodeException(HttpServletResponse.SC_NOT_ACCEPTABLE, ExportErrorCodes.UNSUPPORTED_EXPORT_FORMAT, "The response format topojson requires 'application/json' as accept and content-type headers.");

        } else {
            throw new StatusCodeException(HttpServletResponse.SC_NOT_ACCEPTABLE, ExportErrorCodes.UNSUPPORTED_EXPORT_FORMAT, "The response format %s is not supported".formatted(responseType));
        }
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(final MissingServletRequestParameterException e) {
        return errorHandler.handleStatusCodeException(new MissingParameterException(ExportErrorCodes.MISSING_PARAMETER, e.getParameterName()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMessageConversionException.class, Exception.class})
    public ResponseEntity<Object> handleReadingBodyException(final Exception e) {
        final Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException exception) {
            return errorHandler.handleUnknownParameterException(new UnknownParameterException(ExportErrorCodes.UNKNOWN_PARAMETER, exception.getPropertyName()));
        } else if (cause instanceof InvalidFormatException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(ExportErrorCodes.INVALID_PARAMETER_FORMAT, exception.getValue().toString()));
        } else if (cause instanceof InvalidDefinitionException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(ExportErrorCodes.INVALID_PARAMETER_VALUE, exception.getPath().get(0).getFieldName()));
        } else if (cause instanceof MismatchedInputException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(ExportErrorCodes.MISMATCHED_INPUT, exception.getPath().get(0).getFieldName()));
        } else {
            // 检查是否整体缺少请求体
            if (e.getLocalizedMessage() != null && e.getLocalizedMessage().startsWith("Required request body is missing")) {
                return errorHandler.handleStatusCodeException(new EmptyElementException(ExportErrorCodes.MISSING_PARAMETER, "Request body could not be read"));
            }
            return errorHandler.handleGenericException(e);
        }
    }

    @ExceptionHandler(StatusCodeException.class)
    public ResponseEntity<Object> handleException(final StatusCodeException e) {
        return errorHandler.handleStatusCodeException(e);
    }

    private APIEnums.Profile getProfileEnum(String profile) throws ParameterValueException {
        EncoderNameEnum encoderForProfile = exportService.getEncoderForProfile(profile);
        return APIEnums.Profile.forValue(encoderForProfile.getEncoderName());
    }
}
