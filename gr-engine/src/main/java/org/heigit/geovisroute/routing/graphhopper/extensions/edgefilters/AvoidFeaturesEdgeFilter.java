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
package org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.routing.AvoidFeatureFlags;
import org.heigit.geovisroute.routing.RouteSearchParameters;
import org.heigit.geovisroute.routing.RoutingProfileCategory;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.TollwaysGraphStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.WayCategoryGraphStorage;
import org.heigit.geovisroute.routing.pathprocessors.TollwayExtractor;

public class AvoidFeaturesEdgeFilter implements EdgeFilter {
    private final byte[] buffer;
    private final WayCategoryGraphStorage storage;
    private TollwayExtractor tollwayExtractor;
    private final int avoidFeatureType;

    private static final int NOT_TOLLWAYS = ~AvoidFeatureFlags.TOLLWAYS;

    public AvoidFeaturesEdgeFilter(int profileType, RouteSearchParameters searchParams, BaseGraph graph) throws Exception {
        this.buffer = new byte[10];

        int profileCategory = RoutingProfileCategory.getFromRouteProfile(profileType);
        this.avoidFeatureType = searchParams.getAvoidFeatureTypes() & AvoidFeatureFlags.getProfileFlags(profileCategory);

        storage = GraphStorageUtils.getGraphExtension(graph, WayCategoryGraphStorage.class);
        if (storage == null)
            throw new Exception("ExtendedGraphStorage for avoid features was not found.");

        TollwaysGraphStorage extTollways = GraphStorageUtils.getGraphExtension(graph, TollwaysGraphStorage.class);
        if (extTollways != null)
            tollwayExtractor = new TollwayExtractor(extTollways, searchParams.getProfileType(), searchParams.getProfileParameters());
    }

    public AvoidFeaturesEdgeFilter(int avoidFeatureType, BaseGraph graph) throws Exception {
        if (avoidFeatureType == AvoidFeatureFlags.TOLLWAYS)
            throw new IllegalArgumentException("Invalid constructor for use with feature type: " + AvoidFeatureFlags.TOLLWAYS);
        this.buffer = new byte[10];

        this.avoidFeatureType = avoidFeatureType;

        storage = GraphStorageUtils.getGraphExtension(graph, WayCategoryGraphStorage.class);
        if (storage == null)
            throw new IllegalStateException("ExtendedGraphStorage for avoid features was not found.");
    }

    @Override
    public final boolean accept(EdgeIteratorState iter) {
        if (avoidFeatureType != 0) {
            int edge = iter.getEdge();
            int edgeFeatType = storage.getEdgeValue(edge, buffer);

            if (edgeFeatType != 0) {
                int avoidEdgeFeatureType = avoidFeatureType & edgeFeatType;

                if (avoidEdgeFeatureType != 0) {

                    if ((avoidEdgeFeatureType & NOT_TOLLWAYS) != 0) {
                        // 存在除收费之外的其他限制
                        return false;
                    } else if (tollwayExtractor != null) {
                        // 当该 profile 存在收费时为 false
                        return tollwayExtractor.getValue(edge) == 0;
                    }

                }
            }
        }
        return true;
    }
}
