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

package org.heigit.geovisroute.api.responses.matrix.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.config.EndpointsProperties;
import org.heigit.geovisroute.api.config.SystemMessageProperties;
import org.heigit.geovisroute.api.requests.matrix.MatrixRequest;
import org.heigit.geovisroute.api.responses.matrix.MatrixResponse;
import org.heigit.geovisroute.api.responses.matrix.MatrixResponseInfo;
import org.heigit.geovisroute.matrix.MatrixResult;

@Schema(name = "MatrixResponse", description = "矩阵响应包含每个指定 `metrics` 值对应的一个矩阵。")
public class JSONMatrixResponse extends MatrixResponse {
    public JSONMatrixResponse(MatrixResult result, MatrixRequest request, SystemMessageProperties systemMessageProperties, EndpointsProperties endpointsProperties) {
        super(result, request, systemMessageProperties, endpointsProperties);
    }

    @JsonProperty("matrix")
    @JsonUnwrapped
    public JSONIndividualMatrixResponse getMatrix() {
        return new JSONIndividualMatrixResponse(matrixResult, matrixRequest);
    }

    @JsonProperty("metadata")
    @Schema(description = "关于服务和请求的信息")
    public MatrixResponseInfo getInfo() {
        return responseInformation;
    }
}
