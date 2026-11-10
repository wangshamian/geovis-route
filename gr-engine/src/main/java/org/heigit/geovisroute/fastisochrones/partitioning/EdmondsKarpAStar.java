package org.heigit.geovisroute.fastisochrones.partitioning;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TreeSet;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getSplitValue;

/**
 * 使用双端队列实现的 EdmondsKarp 最大流算法。
 * 寻找从源区域到汇区域的最大可能路径数。
 * 访问顺序由投影顺序决定。距离汇区域更近的节点优先扩展。
 *
 * @author Hendrik Leuschner
 */
public class EdmondsKarpAStar extends MaxFlowMinCut {
    private int srcLimit;
    private int snkLimit;
    private int maxCalls;
    private static final int SINK_NODE_ID = -3;

    public EdmondsKarpAStar(Graph graph, PartitioningData pData, EdgeFilter edgeFilter) {
        super(graph, pData, edgeFilter);
    }

    public boolean hasRemainingCapacity(int edgeId, int nodeId) {
        FlowEdgeData flowEdgeData = pData.getFlowEdgeData(edgeId, nodeId);
        return !flowEdgeData.isFlow();
    }

    public void augment(int edgeId, int baseId, int adjId) {
        FlowEdgeData flowEdgeData = pData.getFlowEdgeData(edgeId, baseId);
        flowEdgeData.setFlow(true);
        FlowEdgeData inverseFlowEdgeData = pData.getFlowEdgeData(flowEdgeData.getInverse(), adjId);
        inverseFlowEdgeData.setFlow(false);
        pData.setFlowEdgeData(edgeId, baseId, flowEdgeData);
        pData.setFlowEdgeData(flowEdgeData.getInverse(), adjId, inverseFlowEdgeData);
    }

    /**
     * 迭代搜索，直到找不到更多连接为止。
     */
    @Override
    public int getMaxFlow() {
        int flow;
        int maxFlow = 0;
        srcLimit = (int) (getSplitValue() * nodes);
        snkLimit = (int) ((1 - getSplitValue()) * nodes);
        Deque<Integer> deque = new ArrayDeque<>(nodes / 2);
        maxCalls = calculateMaxCalls();
        addSrcNodesToDeque(deque);
        do {
            setUnvisitedAll();
            flow = search(deque);
            maxFlow += flow;

            if ((maxFlow > maxFlowLimit)) {
                maxFlow = Integer.MAX_VALUE;
                break;
            }
        } while (flow > 0);
        return maxFlow;
    }

    /**
     * 搜索源集合与汇集合之间的连接。节点扩展顺序由到汇集合的距离决定。
     *
     * @param initialDeque 源集合的双端队列。保持不变，因此只计算一次。
     * @return 找到连接时返回 1，否则返回 0。
     */
    private int search(Deque<Integer> initialDeque) {
        IntObjectHashMap<EdgeInfo> prevMap = new IntObjectHashMap<>((int) Math.ceil(0.1 * nodes));
        Deque<Integer> deque = copyInitialDeque(initialDeque);
        int calls = srcLimit;
        int node;

        while (!deque.isEmpty()) {
            if (calls > maxCalls)
                return 0;
            node = deque.pop();

            if (snkLimit <= nodeOrder.get(node)) {
                prevMap.put(SINK_NODE_ID, new EdgeInfo(-1, node, SINK_NODE_ID));
                //提前停止
                break;
            }
            setVisited(node);
            edgeIterator = edgeExplorer.setBaseNode(node);
            TreeSet<EKEdgeEntry> set = new TreeSet<>(EKEdgeEntry::compareTo);
            while (edgeIterator.next()) {
                calls++;
                int adj = edgeIterator.getAdjNode();
                int edge = edgeIterator.getEdge();
                if (!acceptForPartitioning(edgeIterator)
                        || adj == node
                        || !nodeOrder.containsKey(adj)
                        || !hasRemainingCapacity(edge, node)
                        || isVisited(pData.getVisited(adj)))
                    continue;

                prevMap.put(adj, new EdgeInfo(edge, node, adj));
                set.add(new EKEdgeEntry(adj, this.nodeOrder.get(adj)));
            }
            for (EKEdgeEntry ekEdgeEntry : set)
                deque.push(ekEdgeEntry.getNode());
            calls++;
        }

        return calculateBottleNeck(prevMap);
    }

    private int calculateBottleNeck(IntObjectHashMap<EdgeInfo> prevMap) {
        if (prevMap.getOrDefault(SINK_NODE_ID, null) == null)
            return 0;
        int bottleNeck = Integer.MAX_VALUE;

        EdgeInfo edge = prevMap.getOrDefault(SINK_NODE_ID, null);
        edge = prevMap.getOrDefault(edge.baseNode, null);
        while (edge != null) {

            bottleNeck = Math.min(bottleNeck, hasRemainingCapacity(edge.edge, edge.baseNode) ? 1 : 0);
            if (bottleNeck == 0)
                return 0;
            augment(edge.getEdge(), edge.getBaseNode(), edge.getAdjNode());
            if (nodeOrder.get(edge.baseNode) <= srcLimit)
                break;
            edge = prevMap.getOrDefault(edge.baseNode, null);
        }
        return bottleNeck;
    }

    /**
     * 创建源双端队列。
     *
     * @param deque 要添加源节点的双端队列。
     */
    private void addSrcNodesToDeque(Deque<Integer> deque) {
        //反转插入顺序以最大化 offer 性能
        int nodeNumber = 0;
        while (nodeNumber <= srcLimit) {
            int node = orderedNodes.get(nodeNumber);
            deque.push(node);
            nodeNumber++;
        }
    }

    private Deque<Integer> copyInitialDeque(Deque<Integer> initialDeque) {
        Deque<Integer> deque = new ArrayDeque<>(initialDeque);
        //反转插入顺序以最大化 offer 性能
        int nodeNumber = srcLimit;
        while (nodeNumber >= 0) {
            int node = orderedNodes.get(nodeNumber);
            setVisited(node);
            nodeNumber--;
        }
        return deque;
    }

    private int calculateMaxCalls() {
        int maxBFSCalls = graph.getBaseGraph().getAllEdges().length() * 2;
        double sizeFactor = ((double) nodeOrder.size()) / graph.getBaseGraph().getNodes();
        return (int) Math.ceil(maxBFSCalls * sizeFactor) + nodeOrder.size() * 2;
    }

    private static class EdgeInfo {
        int edge;
        int baseNode;
        int adjNode;

        EdgeInfo(int edge, int baseNode, int adjNode) {
            this.edge = edge;
            this.baseNode = baseNode;
            this.adjNode = adjNode;
        }

        public int getEdge() {
            return edge;
        }

        public int getBaseNode() {
            return baseNode;
        }

        public int getAdjNode() {
            return adjNode;
        }
    }
}

