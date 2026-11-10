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

package org.heigit.geovisroute.api.converters;

import org.heigit.geovisroute.api.APIEnums;
import org.springframework.core.convert.converter.Converter;

public class APIRequestProfileConverter implements Converter<String, APIEnums.Profile> {
    @Override
    public APIEnums.Profile convert(String s) {
        for (APIEnums.Profile profile : APIEnums.Profile.values()) {
            if (profile.toString().equals(s)) {
                return profile;
            }
        }
        // 这是针对该破坏性变更的变通方案：https://github.com/spring-projects/spring-framework/issues/26679
        // 该问题在 spring-boot 3.x.x 中通过 "missingAfterConversion" 标志修复。
        // 在 spring 2.7.x 之前，必填参数存在但格式错误时会引发 HttpMessageConversionException。
        // 在 spring boot 2.7.x 中，它会引发 MissingServletRequestParameterException。
        // 通过在自定义转换器中抛出 RuntimeException，spring 自身现在会引发 HttpMessageConversionException。
        throw new IllegalArgumentException("unsupported profile");
    }
}
