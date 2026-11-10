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

package org.heigit.geovisroute.routing;

/**
 * 用于响应中的警告的持有类
 */
public class RouteWarning {
    public static final int ACCESS_RESTRICTION = 1;
    public static final int TOLLWAYS = 2;
    public static final int SKIPPED_SEGMENTS = 3;
    public static final int SKIPPED_EXTRAS = 4;

    private int warningCode = 0;
    private String warningMessage = "";

    /**
     * 根据传入的警告码生成警告对象并初始化其内容
     *
     * @param warning 应生成的警告对应的警告码
     */
    public RouteWarning(int warning, String additionalInfo) {
        warningCode = warning;
        switch (warning) {
            case ACCESS_RESTRICTION -> warningMessage = "There may be restrictions on some roads";
            case TOLLWAYS -> warningMessage = "There are tollways along the route";
            case SKIPPED_SEGMENTS ->
                    warningMessage = "There are skipped segments along the route. Durations and accessibility may not be correct";
            case SKIPPED_EXTRAS -> warningMessage = "Extra info requested but not available: ".concat(additionalInfo);
            default -> warningMessage = "Unknown error";
        }
    }

    public RouteWarning(int warning) {
        this(warning, "");
    }

    public int getWarningCode() {
        return warningCode;
    }

    public String getWarningMessage() {
        return warningMessage;
    }
}
