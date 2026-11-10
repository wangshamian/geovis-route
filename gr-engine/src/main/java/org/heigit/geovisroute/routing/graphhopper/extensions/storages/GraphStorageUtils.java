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
package org.heigit.geovisroute.routing.graphhopper.extensions.storages;

import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.ExtendedStorageSequence;
import com.graphhopper.storage.GraphExtension;

public class GraphStorageUtils {
    private GraphStorageUtils() {
    }

    public static <T extends GraphExtension> T getGraphExtension(BaseGraph graph, Class<T> type) {
        ExtendedStorageSequence ess = graph.getExtensions();
        GraphExtension[] extensions = ess.getExtensions();
        for (GraphExtension e : extensions) {
            if (type.isInstance(e)) {
                return (T) e;
            }
        }
        return null;
    }
}
