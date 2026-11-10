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
package org.heigit.geovisroute.routing.algorithms;

import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import org.heigit.geovisroute.exceptions.MaxVisitedNodesExceededException;

public abstract class AbstractOneToManyRoutingAlgorithm implements OneToManyRoutingAlgorithm {
    protected final Graph graph;
    protected final Weighting weighting;
    protected final TraversalMode traversalMode;
    protected NodeAccess nodeAccess;
    protected EdgeExplorer inEdgeExplorer;
    protected EdgeExplorer outEdgeExplorer;
    protected int maxVisitedNodes = Integer.MAX_VALUE;
    private EdgeFilter additionalEdgeFilter;

    /**
     * @param graph         指定该算法将在其中运行的图
     * @param weighting     设置所用的权重计算方式（例如 fastest、shortest）。
     * @param traversalMode 图的遍历方式，例如基于 node 还是基于 edge。
     */
    public AbstractOneToManyRoutingAlgorithm(Graph graph, Weighting weighting, TraversalMode traversalMode) {
        this.weighting = weighting;
        this.traversalMode = traversalMode;
        this.graph = graph;
        this.nodeAccess = graph.getNodeAccess();
//        outEdgeExplorer = graph.createEdgeExplorer(AccessFilter.outEdges(flagEncoder.getAccessEnc()));
//        inEdgeExplorer = graph.createEdgeExplorer(AccessFilter.inEdges(flagEncoder.getAccessEnc()));
    }

    @Override
    public void setMaxVisitedNodes(int numberOfNodes) {
        this.maxVisitedNodes = numberOfNodes;
    }

    public AbstractOneToManyRoutingAlgorithm setEdgeFilter(EdgeFilter additionalEdgeFilter) {
        this.additionalEdgeFilter = additionalEdgeFilter;
        return this;
    }

    protected boolean accept(EdgeIterator iter, int prevOrNextEdgeId) {
        return additionalEdgeFilter == null || additionalEdgeFilter.accept(iter);
    }

    protected SPTEntry createSPTEntry(int node, double weight) {
        return new SPTEntry(EdgeIterator.NO_EDGE, node, weight);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getName() + "|" + weighting;
    }

    protected boolean isMaxVisitedNodesExceeded() {
        return maxVisitedNodes < getVisitedNodes();
    }
}
