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

package org.heigit.geovisroute.routing.graphhopper.extensions.storages;

import org.heigit.geovisroute.routing.RouteExtraInfo;
import org.heigit.geovisroute.routing.RouteWarning;

/**
 * 用于声明某个图存储可用于生成警告消息的接口
 */
public interface WarningGraphExtension {
    void setIsUsedForWarning(boolean isWarning);

    boolean isUsedForWarning();

    String getName();

    /**
     * 该方法判断是否应为该图存储生成警告。
     *
     * @param extra 该扩展对应的路线的 {@link RouteExtraInfo} 对象。应使用其值
     *              来判断是否应产生警告。
     * @return 是否应生成警告
     */
    boolean generatesWarning(RouteExtraInfo extra);

    RouteWarning getWarning();
}
