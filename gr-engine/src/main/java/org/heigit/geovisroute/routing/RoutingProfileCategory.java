/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library;
 *  if not, see <https://www.gnu.org/licenses/>.
 */
package org.heigit.geovisroute.routing;

import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.EncodingManager;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;

public class RoutingProfileCategory {
    public static final int UNKNOWN = 0;
    public static final int DRIVING = 1;
    public static final int CYCLING = 2;
    public static final int WALKING = 4;
    public static final int WHEELCHAIR = 8;

    public static int getFromRouteProfile(int profileType) {
        if (RoutingProfileType.isDriving(profileType))
            return RoutingProfileCategory.DRIVING;

        if (RoutingProfileType.isCycling(profileType))
            return RoutingProfileCategory.CYCLING;

        if (RoutingProfileType.isWalking(profileType))
            return RoutingProfileCategory.WALKING;

        if (RoutingProfileType.WHEELCHAIR == profileType)
            return RoutingProfileCategory.WHEELCHAIR;

        return RoutingProfileCategory.UNKNOWN;
    }

    public static int getFromEncoder(EncodingManager encodingManager) {
        if (hasVehicle(encodingManager, FlagEncoderNames.CAR_ORS) || hasVehicle(encodingManager, FlagEncoderNames.HEAVYVEHICLE))
            return RoutingProfileCategory.DRIVING;

        if (hasVehicle(encodingManager, FlagEncoderNames.BIKE_ORS) || hasVehicle(encodingManager, FlagEncoderNames.MTB_ORS) || hasVehicle(encodingManager, FlagEncoderNames.ROADBIKE_ORS)
                || hasVehicle(encodingManager, FlagEncoderNames.BIKE_ELECTRO))
            return RoutingProfileCategory.CYCLING;

        if (hasVehicle(encodingManager, FlagEncoderNames.PEDESTRIAN_ORS) || hasVehicle(encodingManager, FlagEncoderNames.HIKING_ORS))
            return RoutingProfileCategory.WALKING;

        if (encodingManager.hasEncodedValue(FlagEncoderNames.WHEELCHAIR))
            return RoutingProfileCategory.WHEELCHAIR;

        return RoutingProfileCategory.UNKNOWN;
    }

    /**
     * GraphHopper 11 没有携带 encoder 名称本身的编码值。车辆是否存在由其所映射的
     * GraphHopper 车辆的 access 编码值（"&lt;vehicle&gt;_access"）决定。
     */
    private static boolean hasVehicle(EncodingManager encodingManager, String orsEncoderName) {
        return encodingManager.hasEncodedValue(VehicleAccess.key(FlagEncoderNames.getGHVehicleName(orsEncoderName)));
    }

    private RoutingProfileCategory() {
    }
}
