package org.heigit.geovisroute.fastisochrones.partitioning;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getMaxThreadCount;

/**
 * 准备图的分区。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class PreparePartition {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreparePartition.class);
    private final EdgeFilterSequence edgeFilters;
    private final BaseGraph baseGraph;
    private final EncodingManager encodingManager;
    private final IsochroneNodeStorage isochroneNodeStorage;
    private final CellStorage cellStorage;
    private final int nodes;

    public PreparePartition(BaseGraph baseGraph, EncodingManager encodingManager, EdgeFilterSequence edgeFilters) {
        this.baseGraph = baseGraph;
        this.encodingManager = encodingManager;
        this.edgeFilters = edgeFilters;
        this.nodes = baseGraph.getNodes();
        this.isochroneNodeStorage = new IsochroneNodeStorage(this.nodes, baseGraph.getDirectory());
        this.cellStorage = new CellStorage(this.nodes, baseGraph.getDirectory(), isochroneNodeStorage);
    }

    public PreparePartition prepare() {
        //使用 Inertialflow 计算节点 ID 到单元的映射
        int[] nodeCellId = runInertialFlow();
        //识别边界节点
        boolean[] nodeBorderness = calcBorderNodes(nodeCellId);

        //创建并计算按节点排序的等时圈信息
        if (!isochroneNodeStorage.loadExisting()) {
            isochroneNodeStorage.setCellIds(nodeCellId);
            isochroneNodeStorage.setBorderness(nodeBorderness);
            isochroneNodeStorage.flush();
        }

        //按单元排序的信息
        if (!cellStorage.loadExisting()) {
            cellStorage.init();
            cellStorage.calcCellNodesMap();
            cellStorage.flush();
        }
        return this;
    }

    private int[] runInertialFlow() {
        int[] nodeToCellArray = new int[baseGraph.getNodes()];
        ExecutorService threadPool = java.util.concurrent.Executors.newFixedThreadPool(Math.min(getMaxThreadCount(), Runtime.getRuntime().availableProcessors()));
        InverseSemaphore inverseSemaphore = new InverseSemaphore();
        inverseSemaphore.beforeSubmit();
        LOGGER.debug("Submitting task for cell 1");
        threadPool.execute(new InertialFlow(nodeToCellArray, baseGraph, encodingManager, edgeFilters, threadPool, inverseSemaphore));
        try {
            inverseSemaphore.awaitCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(e.getLocalizedMessage());
        }

        threadPool.shutdown();
        return nodeToCellArray;
    }

    private boolean[] calcBorderNodes(int[] nodeCellId) {
        boolean[] nodeBorderness = new boolean[this.nodes];
        boolean borderness = false;
        int adjNode;
        EdgeExplorer ghEdgeExpl = baseGraph.createEdgeExplorer();
        EdgeIterator edgeIter;
        for (int baseNode = 0; baseNode < nodes; baseNode++) {
            edgeIter = ghEdgeExpl.setBaseNode(baseNode);
            while (edgeIter.next()) {
                adjNode = edgeIter.getAdjNode();
                if (nodeCellId[baseNode] != nodeCellId[adjNode]) {
                    borderness = true;
                    break;
                }
            }
            nodeBorderness[baseNode] = borderness;
            borderness = false;
        }
        return nodeBorderness;
    }

    public IsochroneNodeStorage getIsochroneNodeStorage() {
        return isochroneNodeStorage;
    }

    public CellStorage getCellStorage() {
        return cellStorage;
    }
}
