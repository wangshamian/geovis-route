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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.routing.RouteWarning;


@Schema(description = "提示所生成路线可能存在的问题，例如通行限制。会同时生成一个包含受影响分段的相应 `extras` 对象。")
public class JSONWarning {
    @Schema(description = "该警告的标识代码", example = "1")
    @JsonProperty
    private final Integer code;

    @Schema(description = "与该警告关联的消息", example = "This route may go over restricted roads")
    @JsonProperty
    private final String message;

    public JSONWarning(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public JSONWarning(RouteWarning warning) {
        this.code = warning.getWarningCode();
        this.message = warning.getWarningMessage();
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
