package org.heigit.geovisroute.fastisochrones.partitioning;

import com.graphhopper.storage.Graph;

/**
 * 创建运行最大流最小割算法所需的数据。
 *
 * @author Hendrik Leuschner
 */
public class PartitioningDataBuilder {
    private final Graph graph;
    private final PartitioningData pData;

    PartitioningDataBuilder(Graph graph, PartitioningData pData) {
        this.graph = graph;
        this.pData = pData;
    }

    public void run() {
        pData.createEdgeDataStructures(graph.getAllEdges().length() + 1);
        pData.fillFlowEdgeBaseNodes(graph);
        pData.createNodeDataStructures(graph.getNodes() + 1);
    }
}
