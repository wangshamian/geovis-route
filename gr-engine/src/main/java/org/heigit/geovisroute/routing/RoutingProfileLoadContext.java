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

import com.graphhopper.reader.dem.ElevationProvider;

/**
 * 保存 {@link RoutingProfile} 实例之间共享的资源。
 * 这类共享资源的一个例子是 {@link ElevationProvider}。
 */
public class RoutingProfileLoadContext {
    // 在此添加其他共享资源
    private ElevationProvider elevationProvider = null;

    public ElevationProvider getElevationProvider() {
        return elevationProvider;
    }

    public void setElevationProvider(ElevationProvider ep) {
        if (elevationProvider == null) {
            elevationProvider = ep;
        }
    }

    public void releaseElevationProviderCacheAfterAllVehicleProfilesHaveBeenProcessed() {
        if (elevationProvider != null) {
            elevationProvider.release();
        }
    }
}
