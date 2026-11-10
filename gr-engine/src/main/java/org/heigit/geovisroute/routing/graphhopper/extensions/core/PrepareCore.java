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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntContainer;
import com.carrotsearch.hppc.predicates.IntPredicate;
import com.graphhopper.coll.MinHeapWithUpdate;
import com.graphhopper.routing.ch.CHPreparationGraph;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.weighting.AbstractAdjustedWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.*;
import com.graphhopper.util.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;

import static com.graphhopper.routing.ch.CHParameters.CONTRACTED_NODES;
import static com.graphhopper.util.Helper.getMemInfo;

/**
 * 预处理核心区图。核心区图是一种收缩层次图，其中指定的部分不被收缩，
 * 而是保留在最高层级。例如用于根据限制条件构建核心区。
 * <p>
 * 本代码基于 GraphHopper GmbH 的代码。
 *
 * @author Peter Karich
 * @author Hendrik Leuschner, Andrzej Oles
 */
public class PrepareCore extends PrepareContractionHierarchies {
    private final EdgeFilter restrictionFilter;
    private boolean[] restrictedNodes;
    private int restrictedNodesCount = 0;

    private static final int nodesContractedPercentage = 99;

    IntPredicate isCoreNode = new IntPredicate() {
        public boolean apply(int node) {
            return restrictedNodes[node];
        }
    };

    public PrepareCore(BaseGraph baseGraph, CHConfig chConfig, EdgeFilter restrictionFilter) {
        super(baseGraph, chConfig);
        PMap pMap = new PMap(CONTRACTED_NODES + "=" + nodesContractedPercentage);
        setParams(pMap);
        this.restrictionFilter = restrictionFilter;
    }

    @Override
    public CHStorage getCHStore(CHConfig chConfig) {
        if (CHConfig.TYPE_CORE.equals(chConfig.getType()) && graph instanceof GRGraph orsGraph) {
            CHStorage chStore = orsGraph.getCoreStore(chConfig.getName());
            if (chStore == null)
                throw new IllegalArgumentException("There is no Core graph '" + chConfig.getName() + "', existing: " + orsGraph.getCoreGraphNames());
            return chStore;
        }
        return super.getCHStore(chConfig);
    }

    public void initFromGraph() {
        // todo: 这一整套 initFromGraph() 方法链之所以需要，只是因为 PrepareContractionHierarchies 并非
        // 单纯地预处理收缩层次，它还充当某种 '容器'，在 GraphHopper 类中提供对预处理的访问。
        // 若非如此，我们可以把这里整理得干净得多，将变量声明为 final，也不必需要所有这些 close() 方法……
        CorePreparationGraph prepareGraph;
        if (chConfig.getTraversalMode().isEdgeBased()) {
            TurnCostStorage turnCostStorage = graph.getTurnCostStorage();
            if (turnCostStorage == null) {
                throw new IllegalArgumentException("For edge-based CH you need a turn cost storage");
            }
        }
        logger.info("Creating Core graph, {}", getMemInfo());
        prepareGraph = CorePreparationGraph.nodeBased(graph.getNodes(), graph.getEdges());
        nodeContractor = new CoreNodeContractor(prepareGraph, chBuilder, pMap);
        maxLevel = nodes;
        // 我们需要一个内存高效且支持高效更新操作的优先队列
        // TreeMap 内存效率不高，而 PriorityQueue 不支持高效的更新方法
        //（而且内存效率也不高）
        sortedNodes = new MinHeapWithUpdate(prepareGraph.getNodes());
        logger.info("Building Core graph, {}", getMemInfo());
        StopWatch sw = new StopWatch().start();
        Weighting weighting = new RestrictedEdgesWeighting(chConfig.getWeighting(), restrictionFilter);
        buildFromGraph(prepareGraph, graph, weighting);
        logger.info("Finished building Core graph, took: {}s, {}", sw.stop().getSeconds(), getMemInfo());
        nodeContractor.initFromGraph();
        postInit(prepareGraph);
    }

    public void postInit(CHPreparationGraph prepareGraph) {
        restrictedNodes = new boolean[nodes];
        EdgeExplorer restrictionExplorer;
        restrictionExplorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES);//FIXME: 每条边可能被不必要地访问了两次

        for (int node = 0; node < nodes; node++) {
            EdgeIterator edgeIterator = restrictionExplorer.setBaseNode(node);
            while (edgeIterator.next())
                if (!restrictionFilter.accept(edgeIterator))
                    restrictedNodes[node] = restrictedNodes[edgeIterator.getAdjNode()] = true;
        }

        for (int node = 0; node < nodes; node++)
            if (restrictedNodes[node])
                restrictedNodesCount++;
    }

    private static class RestrictedEdgesWeighting extends AbstractAdjustedWeighting {
        private final EdgeFilter restrictionFilter;

        RestrictedEdgesWeighting(Weighting weighting, EdgeFilter restrictionFilter) {
            super(weighting);
            this.restrictionFilter = restrictionFilter;
        }

        public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
            if (restrictionFilter.accept(edgeState))
                return superWeighting.calcEdgeWeight(edgeState, reverse);
            else
                return Double.POSITIVE_INFINITY;
        }

        public String getName() {
            return this.superWeighting.getName();
        }
    }

    public static void buildFromGraph(CorePreparationGraph prepareGraph, Graph graph, Weighting weighting) {
        if (graph.getNodes() != prepareGraph.getNodes())
            throw new IllegalArgumentException("Cannot initialize from given graph. The number of nodes does not match: " +
                    graph.getNodes() + " vs. " + prepareGraph.getNodes());
        if (graph.getEdges() != prepareGraph.getOriginalEdges())
            throw new IllegalArgumentException("Cannot initialize from given graph. The number of edges does not match: " +
                    graph.getEdges() + " vs. " + prepareGraph.getOriginalEdges());
        AllEdgesIterator iter = graph.getAllEdges();
        while (iter.next()) {
            double weightFwd = weighting.calcEdgeWeightWithAccess(iter, false);
            // 使用反向迭代器，因为 RestrictedEdgesWeighting 中的 restrictionFilter.accept 无法在反方向上查询
            EdgeIteratorState iterReverse = graph.getEdgeIteratorStateForKey(GHUtility.reverseEdgeKey(iter.getEdgeKey()));
            double weightBwd = weighting.calcEdgeWeightWithAccess(iterReverse, false);
            int timeFwd = Double.isFinite(weightFwd) ? (int) weighting.calcEdgeMillis(iter, false) : Integer.MAX_VALUE;
            int timeBwd = Double.isFinite(weightBwd) ? (int) weighting.calcEdgeMillis(iter, true) : Integer.MAX_VALUE;
            prepareGraph.addEdge(iter.getBaseNode(), iter.getAdjNode(), iter.getEdge(), weightFwd, weightBwd, timeFwd, timeBwd);
        }
        prepareGraph.prepareForContraction();
    }

    @Override
    protected boolean doNotContract(int node) {
        return super.doNotContract(node) || restrictedNodes[node];
    }

    protected IntContainer contractNode(int node, int level) {
        IntContainer neighbors = super.contractNode(node, level);

        if (neighbors instanceof IntArrayList intArrayList)
            intArrayList.removeAll(isCoreNode);
        else
            throw (new IllegalStateException("Not an isntance of IntArrayList"));

        return neighbors;
    }

    public void finishContractionHook() {
        chStore.setCoreNodes(sortedNodes.size() + restrictedNodesCount);

        // 插入与核心区节点相连的快捷边
        CoreNodeContractor coreNodeContractor = (CoreNodeContractor) nodeContractor;
        while (!sortedNodes.isEmpty())
            coreNodeContractor.insertShortcuts(sortedNodes.poll(), false);
        for (int node = 0; node < nodes; node++)
            if (restrictedNodes[node])
                coreNodeContractor.insertShortcuts(node, false);
    }
}
