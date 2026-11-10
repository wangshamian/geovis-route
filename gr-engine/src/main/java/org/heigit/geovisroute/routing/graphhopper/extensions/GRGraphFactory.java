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
package org.heigit.geovisroute.routing.graphhopper.extensions;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.*;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.builders.GraphStorageBuilder;

import java.util.ArrayList;
import java.util.List;

public class GRGraphFactory {

    private static final Logger LOGGER = Logger.getLogger(GRGraphFactory.class.getName());

    private final List<GraphStorageBuilder> graphStorageBuilders;

    public GRGraphFactory(List<GraphStorageBuilder> graphStorageBuilders) {
        this.graphStorageBuilders = graphStorageBuilders;
    }

    public GRGraph createStorage(GHDirectory dir, GraphHopper gh) {
        EncodingManager encodingManager = gh.getEncodingManager();
        ArrayList<GraphExtension> graphExtensions = new ArrayList<>();

        if (graphStorageBuilders != null) {
            List<GraphStorageBuilder> iterateGraphStorageBuilders = new ArrayList<>(graphStorageBuilders);
            for (GraphStorageBuilder builder : iterateGraphStorageBuilders) {
                try {
                    GraphExtension ext = builder.init(gh);
                    if (ext != null)
                        graphExtensions.add(ext);
                } catch (Exception ex) {
                    graphStorageBuilders.remove(builder);
                    LOGGER.error(ex);
                }
            }
        }

        GRGraph graph = new GRGraph(dir, gh.hasElevation(), true, -1, encodingManager);
        ExtendedStorageSequence extendedStorages = new ExtendedStorageSequence(graphExtensions);
        extendedStorages.init(graph, dir);
        return graph;
    }
}
