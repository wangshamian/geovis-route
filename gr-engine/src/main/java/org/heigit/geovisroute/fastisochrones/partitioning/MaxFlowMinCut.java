package org.heigit.geovisroute.fastisochrones.partitioning;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntIntHashMap;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;

/**
 * 抽象的最大流最小割实现。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public abstract class MaxFlowMinCut {
    protected int maxFlowLimit = Integer.MAX_VALUE;
    protected int nodes;
    protected Graph graph;
    protected EdgeExplorer edgeExplorer;
    protected EdgeIterator edgeIterator;
    protected IntIntHashMap nodeOrder;
    protected IntArrayList orderedNodes;
    protected EdgeFilter edgeFilter;
    PartitioningData pData;
    private int visitedToken;

    MaxFlowMinCut(Graph graph, PartitioningData pData, EdgeFilter edgeFilter) {
        this.graph = graph;
        this.edgeExplorer = this.graph.createEdgeExplorer();
        this.pData = pData;

        setAdditionalEdgeFilter(edgeFilter);
    }

    protected void reset() {
        resetAlgorithm();
        resetData();
    }

    protected void setMaxFlowLimit(int prevMaxFlow) {
        this.maxFlowLimit = prevMaxFlow;
    }

    protected void resetAlgorithm() {
        this.nodes = 0;
        this.visitedToken = 1;
    }

    public void setVisited(int node) {
        pData.setVisited(node, visitedToken);
    }

    public boolean isVisited(int visited) {
        return (visited == visitedToken);
    }

    public void setUnvisitedAll() {
        ++this.visitedToken;
    }

    public abstract int getMaxFlow();

    /**
     * 执行流图的洪泛，并根据 visited 属性确定源集合和汇集合。
     *
     * @return 二分分区
     */
    public BiPartition calcNodePartition() {
        IntHashSet srcSet = new IntHashSet();
        IntHashSet snkSet = new IntHashSet();

        for (int nodeId : nodeOrder.keys) {
            if (isVisited(pData.getVisited(nodeId)))
                srcSet.add(nodeId);
            else
                snkSet.add(nodeId);
        }
        return new BiPartition(srcSet, snkSet);
    }

    /**
     * 为给定节点设置流数据条目
     */
    private void resetData() {
        this.nodes = orderedNodes.size();
        for (int i = 0; i < nodes; i++) {
            int nodeId = orderedNodes.get(i);
            pData.setVisited(nodeId, 0);

            edgeIterator = edgeExplorer.setBaseNode(nodeId);
            while (edgeIterator.next()) {
                if (!acceptForPartitioning(edgeIterator))
                    continue;
                //重置
                FlowEdgeData flowEdgeData = pData.getFlowEdgeData(edgeIterator.getEdge(), edgeIterator.getBaseNode());
                flowEdgeData.setFlow(false);
                pData.setFlowEdgeData(edgeIterator.getEdge(), edgeIterator.getBaseNode(), flowEdgeData);
            }
        }
    }

    public void setNodeOrder() {
        this.nodeOrder = new IntIntHashMap();
        for (int i = 0; i < orderedNodes.size(); i++)
            nodeOrder.put(orderedNodes.get(i), i);
    }

    public void setOrderedNodes(IntArrayList orderedNodes) {
        this.orderedNodes = orderedNodes;
    }

    protected boolean acceptForPartitioning(EdgeIterator edgeIterator) {
        return edgeFilter == null || edgeFilter.accept(edgeIterator);
    }

    public void setAdditionalEdgeFilter(EdgeFilter edgeFilter) {
        this.edgeFilter = edgeFilter;
    }
}
