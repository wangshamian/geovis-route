/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.heigit.geovisroute.fastisochrones;

import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.fastisochrones.storage.BorderNodeDistanceSet;
import org.heigit.geovisroute.fastisochrones.storage.BorderNodeDistanceStorage;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.CORERANGEDIJKSTRA;

/**
 * 受等时圈限制的单源最短路径算法。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class CoreRangeDijkstra extends AbstractIsochroneDijkstra {
    protected IsochroneNodeStorage isochroneNodeStorage;
    protected BorderNodeDistanceStorage borderNodeDistanceStorage;
    private double isochroneLimit = 0;

    public CoreRangeDijkstra(Graph graph, Weighting weighting, IsochroneNodeStorage isochroneNodeStorage, BorderNodeDistanceStorage borderNodeDistanceStorage) {
        super(graph, weighting);
        this.isochroneNodeStorage = isochroneNodeStorage;
        this.borderNodeDistanceStorage = borderNodeDistanceStorage;
    }

    protected void initFrom(int from) {
        currEdge = new SPTEntry(from, 0.0D);
        if (!traversalMode.isEdgeBased()) {
            fromMap.put(from, currEdge);
        }
        fromHeap.add(currEdge);
    }

    protected void runAlgo() {
//        EdgeExplorer explorer = graph.createEdgeExplorer(AccessFilter.outEdges(weighting.getAccessEnc()));
        EdgeExplorer explorer = graph.createEdgeExplorer();
        while (true) {
            visitedNodes++;
            if (isMaxVisitedNodesExceeded() || finished())
                break;

            int baseNode = currEdge.adjNode;
            EdgeIterator iter = explorer.setBaseNode(baseNode);
            while (iter.next()) {
                if (!accept(iter, currEdge.edge))
                    continue;
                int traversalId = traversalMode.createTraversalId(iter, false);
                // Maxim Rylov 的修改：使用 originalEdge 作为 previousEdgeId
                double tmpWeight = weighting.calcEdgeWeight(iter, reverseDirection, currEdge.originalEdge) + currEdge.weight;
                // ORS-GH 修改结束
                if (Double.isInfinite(tmpWeight))
                    continue;

                SPTEntry nEdge = fromMap.get(traversalId);
                if (nEdge == null) {
                    createEntry(iter, traversalId, tmpWeight);
                } else if (nEdge.weight > tmpWeight) {
                    updateEntry(nEdge, iter, tmpWeight);
                }
            }

            /* 仅对核心图节点检查距离是否超过范围限制 ! */
            handleAdjacentBorderNodes(baseNode);

            if (fromHeap.isEmpty())
                break;

            currEdge = fromHeap.poll();
            if (currEdge == null)
                throw new AssertionError("Empty edge cannot happen");
        }
    }

    private void handleAdjacentBorderNodes(int baseNode) {
        if (isochroneNodeStorage.getBorderness(baseNode)) {
            BorderNodeDistanceSet bnds = borderNodeDistanceStorage.getBorderNodeDistanceSet(baseNode);
            for (int i = 0; i < bnds.getAdjBorderNodeIds().length; i++) {
                int id = bnds.getAdjBorderNodeIds()[i];
                double weight = bnds.getAdjBorderNodeDistances()[i] + currEdge.weight;
                if (weight > isochroneLimit || Double.isInfinite(weight))
                    continue;

                SPTEntry nEdge = fromMap.get(id);
                if (nEdge == null) {
                    nEdge = new SPTEntry(EdgeIterator.NO_EDGE, id, weight);
                    nEdge.parent = currEdge;
                    fromMap.put(id, nEdge);
                    fromHeap.add(nEdge);
                } else if (nEdge.weight > weight) {
                    fromHeap.remove(nEdge);
                    nEdge.edge = EdgeIterator.NO_EDGE;
                    nEdge.originalEdge = EdgeIterator.NO_EDGE;
                    nEdge.weight = weight;
                    nEdge.parent = currEdge;
                    fromHeap.add(nEdge);
                }
            }
        }
    }

    protected boolean finished() {
        return isLimitExceeded();
    }

    private boolean isLimitExceeded() {
        return currEdge.getWeightOfVisitedPath() > isochroneLimit;
    }

    public void setIsochroneLimit(double limit) {
        isochroneLimit = limit;
    }

    @Override
    public String getName() {
        return CORERANGEDIJKSTRA;
    }
}
