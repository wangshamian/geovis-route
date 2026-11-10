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
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.RoutingCHEdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.routing.AvoidFeatureFlags;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.WayCategoryGraphStorage;

public class AvoidFeaturesCoreEdgeFilter implements EdgeFilter {
    private final byte[] buffer;
    private final WayCategoryGraphStorage storage;
    private int avoidFeatures;
    private static final String TYPE = "avoid_features";

    public AvoidFeaturesCoreEdgeFilter(BaseGraph baseGraph, int profileCategory) {
        buffer = new byte[10];
        avoidFeatures = AvoidFeatureFlags.getProfileFlags(profileCategory);
        storage = GraphStorageUtils.getGraphExtension(baseGraph, WayCategoryGraphStorage.class);
    }

    public AvoidFeaturesCoreEdgeFilter(BaseGraph baseGraph, int profileCategory, int overrideClass) {
        this(baseGraph, profileCategory);
        avoidFeatures = overrideClass;
    }

    @Override
    public final boolean accept(EdgeIteratorState iter) {
        if (iter instanceof RoutingCHEdgeIterator iterator && iterator.isShortcut())
            return true;
        return (storage.getEdgeValue(iter.getEdge(), buffer) & avoidFeatures) == 0;
    }

    public String getType() {
        return TYPE;
    }

    public int getAvoidFeatures() {
        return avoidFeatures;
    }
}
