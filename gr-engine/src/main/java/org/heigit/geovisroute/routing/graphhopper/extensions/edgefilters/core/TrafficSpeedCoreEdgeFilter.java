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
package org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.TrafficGraphStorage;

/**
 * 该类将分配了交通速度的所有边纳入 core。
 *
 * @author Andrzej Oles
 */

public class TrafficSpeedCoreEdgeFilter implements EdgeFilter {
    private final TrafficGraphStorage trafficGraphStorage;

    public TrafficSpeedCoreEdgeFilter(GRGraph graph) {
        trafficGraphStorage = GraphStorageUtils.getGraphExtension(graph, TrafficGraphStorage.class);
    }

    @Override
    public boolean accept(EdgeIteratorState edge) {
        if (trafficGraphStorage == null)
            return true;

        return !trafficGraphStorage.hasTrafficSpeed(edge.getEdgeKey());
    }

    public static boolean hasTrafficGraphStorage(GRGraph graph) {
        return GraphStorageUtils.getGraphExtension(graph, TrafficGraphStorage.class) != null;
    }
}

