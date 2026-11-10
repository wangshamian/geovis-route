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

import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.*;
import org.heigit.geovisroute.exceptions.MaxVisitedNodesExceededException;

public abstract class AbstractManyToManyRoutingAlgorithm implements ManyToManyRoutingAlgorithm {
    protected final RoutingCHGraph graph;
    protected final Weighting weighting;
    protected final TraversalMode traversalMode;
    protected NodeAccess nodeAccess;
    protected RoutingCHEdgeExplorer inEdgeExplorer;
    protected RoutingCHEdgeExplorer outEdgeExplorer;
    protected int maxVisitedNodes = Integer.MAX_VALUE;
    private CHEdgeFilter additionalEdgeFilter;
    private boolean hasInfiniteUTurnCost;

    /**
     * @param graph         指定该算法将在其中运行的图
     * @param weighting     设置所用的权重计算方式（例如 fastest、shortest）。
     * @param traversalMode 图的遍历方式，例如基于 node 还是基于 edge。
     */
    protected AbstractManyToManyRoutingAlgorithm(RoutingCHGraph graph, Weighting weighting, TraversalMode traversalMode) {
        this.weighting = weighting;
        this.traversalMode = traversalMode;
        this.graph = graph;
        nodeAccess = graph.getBaseGraph().getNodeAccess();
        outEdgeExplorer = graph.createOutEdgeExplorer();
        inEdgeExplorer = graph.createInEdgeExplorer();
    }

    @Override
    public void setMaxVisitedNodes(int numberOfNodes) {
        maxVisitedNodes = numberOfNodes;
    }

    public AbstractManyToManyRoutingAlgorithm setEdgeFilter(CHEdgeFilter additionalEdgeFilter) {
        this.additionalEdgeFilter = additionalEdgeFilter;
        return this;
    }

    protected boolean accept(RoutingCHEdgeIterator iter, int prevOrNextEdgeId, boolean reverse) {
        if (hasInfiniteUTurnCost) {
            if (iter.getEdge() == prevOrNextEdgeId)
                return false;
            if (iter.isShortcut())
                return getIncEdge(iter, !reverse) != prevOrNextEdgeId;
        }
        return additionalEdgeFilter == null || additionalEdgeFilter.accept(iter);
    }

    /**
     * 获取 iter 的进入边。这是 iter 中进入下一条边的最后一条边。
     * 该算法仅使用前向搜索，因此它总是最后一条边，而不是第一条边。
     *
     * @param iter 其边为进入边的迭代器
     * @return 进入边
     */
    protected int getIncEdge(RoutingCHEdgeIteratorState iter, boolean reverse) {
        if (iter.isShortcut()) {
            return reverse ? iter.getSkippedEdge1() : iter.getSkippedEdge2();
        } else {
            return iter.getOrigEdge();
        }
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
        if (getVisitedNodes() > maxVisitedNodes)
            throw new MaxVisitedNodesExceededException();
        return false;
    }

    public void setInfiniteUTurnCost(boolean hasInfiniteUTurnCost) {
        this.hasInfiniteUTurnCost = hasInfiniteUTurnCost;
    }
}
