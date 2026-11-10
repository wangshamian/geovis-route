package org.heigit.geovisroute.matrix;

import com.carrotsearch.hppc.IntHashSet;
import com.graphhopper.routing.querygraph.QueryRoutingCHGraph;
import com.graphhopper.storage.RoutingCHEdgeExplorer;
import com.graphhopper.storage.RoutingCHEdgeIterator;
import com.graphhopper.storage.RoutingCHGraph;
import org.heigit.geovisroute.routing.algorithms.SubGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.ExclusiveDownwardSearchEdgeFilter;

import java.util.PriorityQueue;

import static org.heigit.geovisroute.matrix.util.GraphUtils.isCoreNode;

public class TargetGraphBuilder {
    RoutingCHGraph chGraph;
    private int coreNodeLevel;
    private int nodeCount;

    /**
     * 阶段 I：从所有目标节点向核心构建最短路径树，只沿层级向上。
     * 所使用的 EdgeFilter 是一个带有反向访问接受的下行搜索边过滤器，以便在算法的最后阶段可以向下探索 targetGraph
     *
     * @param targets 构建目标图的种子目标节点
     */
    public TargetGraphResults prepareTargetGraph(int[] targets, RoutingCHGraph chGraph, boolean swap, int coreNodeLevel) {
        PriorityQueue<Integer> localPrioQueue = new PriorityQueue<>(100);
        ExclusiveDownwardSearchEdgeFilter downwardEdgeFilter = new ExclusiveDownwardSearchEdgeFilter(chGraph, swap);
        RoutingCHEdgeExplorer edgeExplorer = swap ? chGraph.createOutEdgeExplorer()
                : chGraph.createInEdgeExplorer();
        SubGraph targetGraph = new SubGraph(chGraph);
        IntHashSet coreExitPoints = new IntHashSet();

        this.coreNodeLevel = coreNodeLevel;
        this.chGraph = chGraph;
        //从基础图中获取节点数，因为 chGraph 应当是带有额外虚拟节点的查询图，这些虚拟节点也计入 chGraph.getNodes()
        //TODO 重构：为更好的风格，在 QueryRoutingCHGraph 中基于底层查询图实现 isVirtualNode
        if (chGraph instanceof QueryRoutingCHGraph)
            this.nodeCount = chGraph.getBaseGraph().getBaseGraph().getNodes();
        else
            this.nodeCount = chGraph.getNodes();

        addNodes(targetGraph, localPrioQueue, targets, coreExitPoints);

        while (!localPrioQueue.isEmpty()) {
            int node = localPrioQueue.poll();
            RoutingCHEdgeIterator iter = edgeExplorer.setBaseNode(node);
            downwardEdgeFilter.setBaseNode(node);
            exploreEntry(targetGraph, localPrioQueue, downwardEdgeFilter, node, iter, coreExitPoints);
        }
        TargetGraphResults targetGraphResults = new TargetGraphResults();
        targetGraphResults.setTargetGraph(targetGraph);
        targetGraphResults.setCoreExitPoints(coreExitPoints);
        return targetGraphResults;
    }

    /**
     * 探索目标图并构建 coreExitPoints
     *
     * @param localPrioQueue
     * @param downwardEdgeFilter
     * @param baseNode
     * @param iter
     */
    private void exploreEntry(SubGraph targetGraph, PriorityQueue<Integer> localPrioQueue, ExclusiveDownwardSearchEdgeFilter downwardEdgeFilter, int baseNode, RoutingCHEdgeIterator iter, IntHashSet coreExitPoints) {
        while (iter.next()) {
            if (!downwardEdgeFilter.accept(iter))
                continue;
            boolean isNewNode = targetGraph.addEdge(baseNode, iter, true);
            int adjNode = iter.getAdjNode();
            if (isCoreNode(chGraph, adjNode, nodeCount, coreNodeLevel))
                coreExitPoints.add(adjNode);
            else if (isNewNode)
                localPrioQueue.add(adjNode);
        }
    }

    /**
     * 向目标图添加节点，并为目标图加入优先队列
     *
     * @param graph
     * @param prioQueue
     * @param nodes
     */
    private void addNodes(SubGraph graph, PriorityQueue<Integer> prioQueue, int[] nodes, IntHashSet coreExitPoints) {
        for (int i = 0; i < nodes.length; i++) {
            int nodeId = nodes[i];
            if (nodeId >= 0) {
                if (graph != null)
                    graph.addEdge(nodeId, null, true);
                if (isCoreNode(chGraph, nodeId, nodeCount, coreNodeLevel))
                    coreExitPoints.add(nodeId);
                else
                    prioQueue.add(nodeId);
            }
        }
    }


    public static class TargetGraphResults {
        SubGraph targetGraph;
        IntHashSet coreExitPoints;

        public SubGraph getTargetGraph() {
            return targetGraph;
        }

        public void setTargetGraph(SubGraph targetGraph) {
            this.targetGraph = targetGraph;
        }

        public IntHashSet getCoreExitPoints() {
            return coreExitPoints;
        }

        public void setCoreExitPoints(IntHashSet coreExitPoints) {
            this.coreExitPoints = coreExitPoints;
        }
    }
}
