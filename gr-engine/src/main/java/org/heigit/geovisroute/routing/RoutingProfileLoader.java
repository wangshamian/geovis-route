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

import org.heigit.geovisroute.config.EngineProperties;
import org.heigit.geovisroute.config.profile.ProfileProperties;

import java.util.concurrent.Callable;

/**
 * Callable，根据名称字符串、{@link ProfileProperties}、{@link EngineProperties}
 * 和 {@link RoutingProfileLoadContext} 创建 {@link RoutingProfile}。
 */
public class RoutingProfileLoader implements Callable<RoutingProfile> {
    private final String name;
    private final EngineProperties engineConfig;
    private final ProfileProperties profile;
    private final RoutingProfileLoadContext loadContext;
    private final String graphVersion;

    public RoutingProfileLoader(String name, ProfileProperties profile, EngineProperties engineConfig, String graphVersion, RoutingProfileLoadContext loadContext) {
        this.name = name;
        this.graphVersion = graphVersion;
        this.profile = profile;
        this.engineConfig = engineConfig;
        this.loadContext = loadContext;
    }

    @Override
    public RoutingProfile call() throws Exception {
        Thread.currentThread().setName("ORS-pl-" + name);
        return new RoutingProfile(name, profile, engineConfig, graphVersion, loadContext);
    }
}