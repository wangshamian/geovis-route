package org.heigit.geovisroute.fastisochrones;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.fastisochrones.storage.BorderNodeDistanceSet;
import org.heigit.geovisroute.fastisochrones.storage.BorderNodeDistanceStorage;
import org.heigit.geovisroute.fastisochrones.storage.EccentricityStorage;
import org.heigit.geovisroute.routing.algorithms.DijkstraOneToManyAlgorithm;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getMaxCellNodesNumber;
import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getMaxThreadCount;

/**
 * 偏心度的实现。在给定起始边界节点的情况下，计算单元内所有最短路径的最大值。
 * 此外还计算边界节点之间的所有距离对。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class Eccentricity extends AbstractEccentricity {
    //此值决定单元中需要到达多少个节点，该单元才算作完全可达。
    //有些节点可能属于某个单元但不可达（不连通、权重无限等）
    private static final double ACCEPTED_FULLY_REACHABLE_PERCENTAGE = 0.995;
    //此因子决定当无法在单元内找到偏心度时，Dijkstra 应搜索多远来找到路径。
    //因子为 10 表示 Dijkstra 会搜索 10 * maxCellNodesNumber 的区域。
    //这有助于更好地估计偏心度，同时避免为找到它而在整个图上运行 Dijkstra。
    private static final int ECCENTRICITY_DIJKSTRA_LIMIT_FACTOR = 10;
    private final LocationIndex locationIndex;

    public Eccentricity(BaseGraph baseGraph, LocationIndex locationIndex, IsochroneNodeStorage isochroneNodeStorage, CellStorage cellStorage) {
        super(baseGraph);
        this.locationIndex = locationIndex;
        this.isochroneNodeStorage = isochroneNodeStorage;
        this.cellStorage = cellStorage;
    }

    public void calcEccentricities(Weighting weighting, EdgeFilter additionalEdgeFilter, AbstractAccessParser accessParser) {
        if (eccentricityStorages == null) {
            eccentricityStorages = new ArrayList<>();
        }
        EccentricityStorage eccentricityStorage = getEccentricityStorage(weighting);
        if (!eccentricityStorage.loadExisting())
            eccentricityStorage.init();
        ExecutorService threadPool = java.util.concurrent.Executors.newFixedThreadPool(Math.min(getMaxThreadCount(), Runtime.getRuntime().availableProcessors()));

        ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(threadPool);

        EdgeFilter defaultEdgeFilter = AccessFilter.outEdges(accessParser.getAccessEnc());

        IntObjectHashMap<IntHashSet> relevantNodesSets = new IntObjectHashMap<>(isochroneNodeStorage.getCellIds().size());
        for (IntCursor cellId : isochroneNodeStorage.getCellIds()) {
            relevantNodesSets.put(cellId.value, getRelevantContourNodes(cellId.value, cellStorage, isochroneNodeStorage));
        }

        //通过 RangeDijkstra 计算偏心度
        int borderNodeCount = 0;
        for (int borderNode = 0; borderNode < baseGraph.getNodes(); borderNode++) {
            if (!isochroneNodeStorage.getBorderness(borderNode))
                continue;
            final int node = borderNode;
            borderNodeCount++;
            completionService.submit(() -> {
                //首先仅在单元内运行 dijkstra，尝试找到单元中的_所有_节点
                EdgeFilterSequence edgeFilterSequence = new EdgeFilterSequence();
                FixedCellEdgeFilter fixedCellEdgeFilter = new FixedCellEdgeFilter(isochroneNodeStorage, isochroneNodeStorage.getCellId(node), baseGraph.getNodes());
                edgeFilterSequence.add(defaultEdgeFilter);
                edgeFilterSequence.add(fixedCellEdgeFilter);
                edgeFilterSequence.add(additionalEdgeFilter);
                RangeDijkstra rangeDijkstra = new RangeDijkstra(baseGraph, weighting);
                rangeDijkstra.setMaxVisitedNodes(getMaxCellNodesNumber() * ECCENTRICITY_DIJKSTRA_LIMIT_FACTOR);
                rangeDijkstra.setEdgeFilter(edgeFilterSequence);
                rangeDijkstra.setCellNodes(cellStorage.getNodesOfCell(isochroneNodeStorage.getCellId(node)));
                double eccentricity = rangeDijkstra.calcMaxWeight(node, relevantNodesSets.get(isochroneNodeStorage.getCellId(node)));
                int cellNodeCount = cellStorage.getNodesOfCell(isochroneNodeStorage.getCellId(node)).size();
                //如果第一次运行中找到的节点不够多，则在单元外重新运行，但尽量找到几乎所有节点
                //有时单元中的节点无法找到，但我们不想每次都搜索整个图，因此限制 Dijkstra
                if (((double) rangeDijkstra.getFoundCellNodeSize()) / cellNodeCount < ACCEPTED_FULLY_REACHABLE_PERCENTAGE) {
                    rangeDijkstra = new RangeDijkstra(baseGraph, weighting);
                    rangeDijkstra.setMaxVisitedNodes(getMaxCellNodesNumber() * ECCENTRICITY_DIJKSTRA_LIMIT_FACTOR);
                    rangeDijkstra.setEdgeFilter(edgeFilterSequence);
                    rangeDijkstra.setCellNodes(cellStorage.getNodesOfCell(isochroneNodeStorage.getCellId(node)));
                    edgeFilterSequence = new EdgeFilterSequence();
                    edgeFilterSequence.add(defaultEdgeFilter);
                    rangeDijkstra.setEdgeFilter(edgeFilterSequence);
                    eccentricity = rangeDijkstra.calcMaxWeight(node, relevantNodesSets.get(isochroneNodeStorage.getCellId(node)));
                }

                //TODO 也许可以实现比使用较高百分比作为 acceptedFullyReachable 更聪明的逻辑
                boolean isFullyReachable = ((double) rangeDijkstra.getFoundCellNodeSize()) / cellNodeCount >= ACCEPTED_FULLY_REACHABLE_PERCENTAGE;
                eccentricityStorage.setFullyReachable(node, isFullyReachable);

                eccentricityStorage.setEccentricity(node, eccentricity);
            }, String.valueOf(node));
        }

        threadPool.shutdown();

        try {
            for (int i = 0; i < borderNodeCount; i++) {
                completionService.take().get();
            }
        } catch (Exception e) {
            threadPool.shutdownNow();
            throw new RuntimeException(e);
        }

        eccentricityStorage.storeBorderNodeToPointerMap();
        eccentricityStorage.flush();
    }

    public void calcBorderNodeDistances(Weighting weighting, EdgeFilter additionalEdgeFilter, AbstractAccessParser accessParser) {
        if (borderNodeDistanceStorages == null) {
            borderNodeDistanceStorages = new ArrayList<>();
        }
        BorderNodeDistanceStorage borderNodeDistanceStorage = getBorderNodeDistanceStorage(weighting);
        if (!borderNodeDistanceStorage.loadExisting())
            borderNodeDistanceStorage.init();

        ExecutorService threadPool = java.util.concurrent.Executors.newFixedThreadPool(Math.min(getMaxThreadCount(), Runtime.getRuntime().availableProcessors()));
        ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(threadPool);

        int cellCount = 0;
        for (IntCursor cellId : isochroneNodeStorage.getCellIds()) {
            final int currentCellId = cellId.value;
            cellCount++;
            completionService.submit(() -> calculateBorderNodeDistances(borderNodeDistanceStorage, additionalEdgeFilter, currentCellId, weighting, accessParser), String.valueOf(currentCellId));
        }

        threadPool.shutdown();

        try {
            for (int i = 0; i < cellCount; i++) {
                completionService.take().get();
            }
        } catch (Exception e) {
            threadPool.shutdownNow();
            throw new RuntimeException(e);
        }
        borderNodeDistanceStorage.storeBorderNodeToPointerMap();
        borderNodeDistanceStorage.flush();
    }

    private void calculateBorderNodeDistances(BorderNodeDistanceStorage borderNodeDistanceStorage, EdgeFilter additionalEdgeFilter, int cellId, Weighting weighting, AbstractAccessParser accessParser) {
        int[] cellBorderNodes = getBorderNodesOfCell(cellId, cellStorage, isochroneNodeStorage).toArray();
        EdgeFilterSequence edgeFilterSequence = new EdgeFilterSequence();
        EdgeFilter defaultEdgeFilter = AccessFilter.outEdges(accessParser.getAccessEnc());
        edgeFilterSequence.add(defaultEdgeFilter);
        edgeFilterSequence.add(additionalEdgeFilter);
        Graph graph = baseGraph.getBaseGraph();

        for (int borderNode : cellBorderNodes) {
            DijkstraOneToManyAlgorithm algorithm = new DijkstraOneToManyAlgorithm(graph, weighting, TraversalMode.NODE_BASED);
            algorithm.setEdgeFilter(edgeFilterSequence);
            algorithm.prepare(new int[]{borderNode}, cellBorderNodes);
            algorithm.setMaxVisitedNodes(getMaxCellNodesNumber() * 20);
            SPTEntry[] targets = algorithm.calcPaths(borderNode, cellBorderNodes);
            int[] ids = new int[targets.length - 1];
            double[] distances = new double[targets.length - 1];
            int index = 0;
            for (int i = 0; i < targets.length; i++) {
                if (cellBorderNodes[i] == borderNode)
                    continue;
                ids[index] = cellBorderNodes[i];
                if (targets[i] == null) {
                    distances[index] = Double.POSITIVE_INFINITY;
                } else if (targets[i].adjNode == borderNode) {
                    distances[index] = 0;
                } else
                    distances[index] = targets[i].weight;
                index++;
            }
            borderNodeDistanceStorage.storeBorderNodeDistanceSet(borderNode, new BorderNodeDistanceSet(ids, distances));
        }
    }

    private IntHashSet getBorderNodesOfCell(int cellId, CellStorage cellStorage, IsochroneNodeStorage isochroneNodeStorage) {
        IntHashSet borderNodes = new IntHashSet();
        for (IntCursor node : cellStorage.getNodesOfCell(cellId)) {
            if (isochroneNodeStorage.getBorderness(node.value))
                borderNodes.add(node.value);
        }
        return borderNodes;
    }

    private IntHashSet getRelevantContourNodes(int cellId, CellStorage cellStorage, IsochroneNodeStorage isochroneNodeStorage) {
        if (this.locationIndex == null)
            return cellStorage.getNodesOfCell(cellId);
        List<Double> contourCoordinates = cellStorage.getCellContourOrder(cellId);
        FixedCellEdgeFilter fixedCellEdgeFilter = new FixedCellEdgeFilter(isochroneNodeStorage, cellId, Integer.MAX_VALUE);
        int j = 0;
        IntHashSet contourNodes = new IntHashSet();
        while (j < contourCoordinates.size()) {
            double latitude = contourCoordinates.get(j);
            j++;
            double longitude = contourCoordinates.get(j);
            j++;
            int nodeId = locationIndex.findClosest(latitude, longitude, fixedCellEdgeFilter).getClosestNode();
            contourNodes.add(nodeId);
        }
        return contourNodes;
    }
}
