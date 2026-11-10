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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.heigit.geovisroute.api.APIEnums;
import org.heigit.geovisroute.api.config.EndpointsProperties;
import org.heigit.geovisroute.api.config.SystemMessageProperties;
import org.heigit.geovisroute.api.errors.CommonResponseEntityExceptionHandler;
import org.heigit.geovisroute.api.requests.matrix.MatrixRequest;
import org.heigit.geovisroute.api.responses.matrix.json.JSONMatrixResponse;
import org.heigit.geovisroute.api.services.MatrixService;
import org.heigit.geovisroute.common.EncoderNameEnum;
import org.heigit.geovisroute.exceptions.*;
import org.heigit.geovisroute.matrix.MatrixErrorCodes;
import org.heigit.geovisroute.matrix.MatrixResult;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Matrix Service", description = "获取时间与距离的一对多、多对一和多对多矩阵")
@RequestMapping("/v2/matrix")
@ApiResponse(responseCode = "400", description = "请求不正确，因此无法处理。")
@ApiResponse(responseCode = "404", description = "无法找到某个元素。如有可能，会提供更详细的错误码。")
@ApiResponse(responseCode = "405", description = "不支持指定的 HTTP 方法。更多细节请参阅端点文档。")
@ApiResponse(responseCode = "413", description = "请求体超过服务器可处理的大小，请求中提供的数据超出了容量限制。")
@ApiResponse(responseCode = "500", description = "遇到意外错误，会提供更详细的错误码。")
@ApiResponse(responseCode = "501", description = "表示服务器不支持满足该请求所需的功能。")
@ApiResponse(responseCode = "503", description = "服务器当前因过载或维护而不可用。")
public class MatrixAPI {
    static final CommonResponseEntityExceptionHandler errorHandler = new CommonResponseEntityExceptionHandler(MatrixErrorCodes.BASE);
    private final EndpointsProperties endpointsProperties;
    private final SystemMessageProperties systemMessageProperties;
    private final MatrixService matrixService;

    public MatrixAPI(EndpointsProperties endpointsProperties, SystemMessageProperties systemMessageProperties, MatrixService matrixService) {
        this.endpointsProperties = endpointsProperties;
        this.systemMessageProperties = systemMessageProperties;
        this.matrixService = matrixService;
    }

    // 通用捕获方法 - 当 URL 中提供额外信息时，会访问其他方法。
    @GetMapping
    @Operation(hidden = true)
    public void getGetMapping() throws MissingParameterException {
        throw new MissingParameterException(MatrixErrorCodes.MISSING_PARAMETER, "profile");
    }

    @PostMapping
    @Operation(hidden = true)
    public String getPostMapping() throws MissingParameterException {
        throw new MissingParameterException(MatrixErrorCodes.MISSING_PARAMETER, "profile");
    }

    // 匹配任何未定义的响应类型
    @PostMapping(value = "/{profile}/*")
    @Operation(hidden = true)
    public void getInvalidResponseType() throws StatusCodeException {
        throw new StatusCodeException(HttpServletResponse.SC_NOT_ACCEPTABLE, MatrixErrorCodes.UNSUPPORTED_EXPORT_FORMAT, "This response format is not supported");
    }

    // 功能请求方法

    @PostMapping(value = "/{profile}", produces = {"application/json;charset=UTF-8"})
    @Operation(
            description = """
                    返回多个起点与终点之间的耗时或距离矩阵。
                    默认返回方形的耗时矩阵，其中 locations 中的每个点都与其它各点两两配对。若某个值无法确定，则结果为 null。\
                    """,
            summary = "Matrix Service"
    )
    @ApiResponse(
            responseCode = "200",
            description = "请求成功处理的标准响应。返回 JSON。",
            content = {@Content(
                    mediaType = "application/json;charset=UTF-8",
                    schema = @Schema(implementation = JSONMatrixResponse.class)
            )
            })
    public JSONMatrixResponse getDefault(@Parameter(name = "profile", description = "指定矩阵 profile。", required = true, example = "driving-car") @PathVariable String profile,
                                         @Parameter(description = "请求负载", required = true) @RequestBody MatrixRequest request) throws StatusCodeException {
        return getJsonMime(profile, request);
    }

    @PostMapping(value = "/{profile}/json", produces = {"application/json;charset=UTF-8"})
    @Operation(
            summary = "使用指定 profile 进行矩阵计算",
            hidden = true
    )
    @ApiResponse(
            responseCode = "200",
            description = "请求成功处理的标准响应。返回 JSON。",
            content = {@Content(
                    mediaType = "application/json;charset=UTF-8",
                    array = @ArraySchema(schema = @Schema(implementation = JSONMatrixResponse.class))
            )
            })
    public JSONMatrixResponse getJsonMime(
            @Parameter(description = "指定矩阵 profile。", required = true, example = "driving-car") @PathVariable String profile,
            @Parameter(description = "请求负载", required = true) @RequestBody MatrixRequest originalRequest) throws StatusCodeException {
        originalRequest.setProfile(getProfileEnum(profile));
        originalRequest.setProfileName(profile);
        originalRequest.setResponseType(APIEnums.MatrixResponseType.JSON);
        MatrixResult matrixResult = matrixService.generateMatrixFromRequest(originalRequest);

        return new JSONMatrixResponse(matrixResult, originalRequest, systemMessageProperties, endpointsProperties);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(final MissingServletRequestParameterException e) {
        return errorHandler.handleStatusCodeException(new MissingParameterException(MatrixErrorCodes.MISSING_PARAMETER, e.getParameterName()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConversionFailedException.class, HttpMessageConversionException.class, Exception.class})
    public ResponseEntity<Object> handleReadingBodyException(final Exception e) {
        final Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException exception) {
            return errorHandler.handleUnknownParameterException(new UnknownParameterException(MatrixErrorCodes.UNKNOWN_PARAMETER, exception.getPropertyName()));
        } else if (cause instanceof InvalidFormatException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_FORMAT, "" + exception.getValue()));
        } else if (cause instanceof ConversionFailedException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "" + exception.getValue()));
        } else if (cause instanceof InvalidDefinitionException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, exception.getPath().get(0).getFieldName()));
        } else if (cause instanceof MismatchedInputException exception) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_FORMAT, exception.getPath().get(0).getFieldName()));
        } else {
            // 检查是否整体缺少请求体
            if (e.getLocalizedMessage() != null && e.getLocalizedMessage().startsWith("Required request body is missing")) {
                return errorHandler.handleStatusCodeException(new EmptyElementException(MatrixErrorCodes.MISSING_PARAMETER, "Request body could not be read"));
            }
            return errorHandler.handleGenericException(e);
        }
    }

    @ExceptionHandler(StatusCodeException.class)
    public ResponseEntity<Object> handleException(final StatusCodeException e) {
        return errorHandler.handleStatusCodeException(e);
    }

    private APIEnums.Profile getProfileEnum(String profile) throws ParameterValueException {
        EncoderNameEnum encoderForProfile = matrixService.getEncoderForProfile(profile);
        return APIEnums.Profile.forValue(encoderForProfile.getEncoderName());
    }
}
