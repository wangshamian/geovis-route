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
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.BordersGraphStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;

public class AvoidBordersCoreEdgeFilter implements EdgeFilter {
    private final BordersGraphStorage storage;
    private int[] avoidCountries;
    private boolean isAvoidCountries = false;

    //用于避让所有国界
    public AvoidBordersCoreEdgeFilter(BaseGraph baseGraph) {
        this.storage = GraphStorageUtils.getGraphExtension(baseGraph, BordersGraphStorage.class);
    }

    //用于指定要避让的多个国家（针对特定的 LM 集合）
    public AvoidBordersCoreEdgeFilter(BaseGraph baseGraph, int[] avoidCountries) {
        this.storage = GraphStorageUtils.getGraphExtension(baseGraph, BordersGraphStorage.class);
        this.avoidCountries = avoidCountries;
        if (avoidCountries.length > 0) isAvoidCountries = true;
    }

    public int[] getAvoidCountries() {
        return avoidCountries;
    }

    /**
     * 判断某条边是否应被过滤
     *
     * @param iter 指向给定边的迭代器
     * @return <tt>true</tt> 当且仅当迭代器指向的边不被过滤
     */
    @Override
    public final boolean accept(EdgeIteratorState iter) {
        //如果指定了特定国家，只需检查它是否是其中之一的国界
        if (iter instanceof RoutingCHEdgeIterator iterator && iterator.isShortcut())
            return true;
        if (isAvoidCountries)
            return !restrictedCountry(iter.getEdge());
        //否则检查是否存在任何国界
        if (storage == null) {
            return true;
        } else {
            return storage.getEdgeValue(iter.getEdge(), BordersGraphStorage.Property.TYPE) == BordersGraphStorage.NO_BORDER;
        }

    }

    public boolean restrictedCountry(int edgeId) {
        int startCountry = storage.getEdgeValue(edgeId, BordersGraphStorage.Property.START);
        int endCountry = storage.getEdgeValue(edgeId, BordersGraphStorage.Property.END);

        for (int i = 0; i < avoidCountries.length; i++) {
            if (startCountry == avoidCountries[i] || endCountry == avoidCountries[i]) {
                return true;
            }
        }

        return false;
    }
}
