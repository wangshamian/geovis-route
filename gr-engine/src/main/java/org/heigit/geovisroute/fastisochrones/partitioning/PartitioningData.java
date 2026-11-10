package org.heigit.geovisroute.fastisochrones.partitioning;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.storage.Graph;

import java.util.Arrays;

/**
 * 用于最大流最小割数据的临时预处理存储。
 *
 * @author Hendrik Leuschner
 */
public class PartitioningData {
    //边数据
    //正向边和反向边的边 ID 相同，但我们需要为它们分别保存不同的数据，因此每个 edgeId 在 x 和 x+1 处都有一个条目
    int[] flowEdgeBaseNode;
    boolean[] flow;
    //节点数据
    int[] visited;

    public PartitioningData() {
    }

    public PartitioningData(int[] flowEdgeBaseNode, boolean[] flow, int[] visited) {
        this.flowEdgeBaseNode = flowEdgeBaseNode;
        this.flow = flow;
        this.visited = visited;
    }

    public void createEdgeDataStructures(int size) {
        if (size < 1 || size > Integer.MAX_VALUE / 2)
            throw new IllegalArgumentException("Bad size.");
        flowEdgeBaseNode = new int[2 * size];
        flow = new boolean[2 * size];
        Arrays.fill(flowEdgeBaseNode, -1);
    }

    public void fillFlowEdgeBaseNodes(Graph graph) {
        AllEdgesIterator iter = graph.getAllEdges();
        while (iter.next()) {
            flowEdgeBaseNode[2 * iter.getEdge()] = iter.getBaseNode();
            flowEdgeBaseNode[2 * iter.getEdge() + 1] = iter.getAdjNode();
        }
    }

    public void createNodeDataStructures(int size) {

        if (size < 1 || size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Bad size.");
        visited = new int[size];
    }

    public void setFlowEdgeData(int edgeId, int baseNode, FlowEdgeData data) {
        if (flowEdgeBaseNode[2 * edgeId] == baseNode) {
            setFlowEdgeData(2 * edgeId, data);
            return;
        } else if (flowEdgeBaseNode[2 * edgeId + 1] == baseNode) {
            setFlowEdgeData(2 * edgeId + 1, data);
            return;
        }
        throw new IllegalStateException("edgeId " + edgeId + " and basenode " + baseNode + " do not belong together");
    }

    public FlowEdgeData getFlowEdgeData(int edgeId, int baseNode) {
        int pointer = -1;
        if (flowEdgeBaseNode[2 * edgeId] == baseNode)
            pointer = 2 * edgeId;
        if (flowEdgeBaseNode[2 * edgeId + 1] == baseNode)
            pointer = 2 * edgeId + 1;
        if (pointer == -1)
            throw new IllegalStateException("Edge " + edgeId + " and node " + baseNode + " do not belong together?");
        return new FlowEdgeData(flow[pointer], edgeId);
    }

    private void setFlowEdgeData(int dataPosition, FlowEdgeData data) {
        if (dataPosition > flow.length - 1 || dataPosition < 0)
            throw new IllegalArgumentException("Index " + dataPosition + " out of bounds for flow with length " + flow.length);
        flow[dataPosition] = data.isFlow();
    }

    public void setVisited(int nodeId, int newVisited) {
        if (nodeId > visited.length - 1 || nodeId < 0)
            throw new IllegalArgumentException("Index " + nodeId + " out of bounds for visited with length " + visited.length);
        visited[nodeId] = newVisited;
    }

    public int getVisited(int nodeId) {
        return visited[nodeId];
    }
}
