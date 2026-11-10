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
package org.heigit.geovisroute.routing.graphhopper.extensions.core;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.RoutingCHEdgeIteratorState;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

public class TDCorePathExtractor extends CorePathExtractor {

    public static Path extractPath(RoutingCHGraph graph, Weighting weighting, SPTEntry fwdEntry, SPTEntry bwdEntry, double weight) {
        return (new TDCorePathExtractor(graph, weighting)).extract(fwdEntry, bwdEntry, weight);
    }

    protected TDCorePathExtractor(RoutingCHGraph routingGraph, Weighting weighting) {
        super(routingGraph, weighting);
    }

    @Override
    protected void onMeetingPoint(int inEdge, int viaNode, int outEdge) {
        // 无需在交汇点处理任何转向
    }

    @Override
    protected SPTEntry followParentsUntilRoot(SPTEntry sptEntry, boolean reverse) {
        SPTEntry currEntry = sptEntry;
        SPTEntry parentEntry = currEntry.parent;
        while (EdgeIterator.Edge.isValid(currEntry.edge)) {
            onTdEdge(currEntry, reverse); // 这里 TD 与 DefaultBidirPathExtractor 不同
            currEntry = parentEntry;
            parentEntry = currEntry.parent;
        }
        return currEntry;
    }

    private void onTdEdge(SPTEntry currEdge, boolean bwd) {
        int edgeId = currEdge.edge;
        int adjNode = currEdge.adjNode;
        RoutingCHEdgeIteratorState edgeState = getRoutingGraph().getEdgeIteratorState(edgeId, adjNode);

        // 快捷边只包含有效的权重，因此在计入距离和时间之前先展开
        if (edgeState.isShortcut()) {
            int edge = currEdge.parent.edge;
            onEdge(edgeId, adjNode, bwd, edge);
        } else {
            EdgeIteratorState edge = getRoutingGraph().getBaseGraph().getEdgeIteratorState(edgeState.getOrigEdge(), edgeState.getAdjNode());
            path.addDistance(edge.getDistance());
            path.addTime((bwd ? -1 : 1) * (currEdge.time - currEdge.parent.time));
            path.addEdge(edge.getEdge());
        }
    }

}
