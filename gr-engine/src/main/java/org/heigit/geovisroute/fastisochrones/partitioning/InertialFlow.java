package org.heigit.geovisroute.fastisochrones.partitioning;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import org.heigit.geovisroute.fastisochrones.partitioning.Projector.Projection;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getMaxCellNodesNumber;
import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getMinCellNodesNumber;

/**
 * InertialFlow 算法的递归实现，用于对图进行分区。
 *
 * @author Hendrik Leuschner
 */
public class InertialFlow implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InertialFlow.class);
    private static final int MIN_SPLITTING_ITERATION = 0;
    private static final int MAX_SPLITTING_ITERATION = Integer.MAX_VALUE; //==2^32
    //定义在最后一步 InertialFlow 之后一个单元可拆分成的非连通单元数量。
    //使用 10 作为估计值，因为大多数单元会被拆分为 1 - 5 个独立单元。
    //如果该值过高且图中存在（例如因数据错误导致的）大量非连通节点，我们不希望为所有这些节点单独创建单元
    private static final int MAX_SUBCELL_NUMBER = 10;
    private static final boolean SEPARATEDISCONNECTED = true;
    //投影会在最大流算法之前进行评估。由于最大流计算相对昂贵，只有最佳的 CONSIDERED_PROJECTIONS 个投影会真正运行该算法
    private static final int CONSIDERED_PROJECTIONS = 3;
    private static final Projector projector = new Projector();
    private static String encodeName;
    protected Map<Projection, IntArrayList> projections;
    private int cellId;
    private BaseGraph graph;
    private EdgeFilter edgeFilter;
    private PartitioningData pData;
    private int[] nodeToCellArr;
    private ExecutorService executorService;
    private InverseSemaphore inverseSemaphore;
    private EncodedValue encodedValue;

    public InertialFlow(int[] nodeToCellArray, BaseGraph graph, EncodingManager encodingManager, EdgeFilterSequence edgeFilters, ExecutorService executorService, InverseSemaphore inverseSemaphore) {
        //从 cellId 1 开始，这样对其进行位移时不会在前面产生零
        setNodeToCellArr(nodeToCellArray);
        setCellId(1);
        setGraph(graph);
        setEdgeFilter(edgeFilters);
        setExecutorService(executorService);
        setInverseSemaphore(inverseSemaphore);
        setEncodedValue(encodingManager.getEncodedValues().get(0));

        PartitioningData partitioningData = new PartitioningData();
        PartitioningDataBuilder partitioningDataBuilder = new PartitioningDataBuilder(graph, partitioningData);
        partitioningDataBuilder.run();
        setPartitioningData(partitioningData);
        projector.setGraph(graph);
        setProjections(projector.calculateProjections());

        LOGGER.info("Number of nodes: {}", graph.getNodes());
        LOGGER.info("Number of edges: {}", graph.getAllEdges().length());
    }

    private InertialFlow() {
    }

    /**
     * 拆分图。按此拆分对投影排序。运行递归。
     */
    public void run() {
        try {
            BiPartition biPartition = graphBiSplit(this.projections);
            BiPartitionProjection biPartitionProjection = projector.partitionProjections(this.projections, biPartition);
            this.projections = null;
            recursion(getInvokeNextOrSaveResult(biPartition), biPartition.getPartition(0).size(), biPartition.getPartition(1).size(), biPartitionProjection);
        } finally {
            inverseSemaphore.taskCompleted();
        }
    }

    /**
     * 根据最大流最小割算法将节点集合拆分为两个节点集合。
     *
     * @param projections 待拆分节点的投影。
     * @return
     */
    private BiPartition graphBiSplit(Map<Projection, IntArrayList> projections) {
        //估计的最大迭代次数
        int mincutScore = graph.getBaseGraph().getAllEdges().length();
        double sizeFactor = ((double) projections.get(Projection.LINE_M00).size()) / graph.getNodes();
        mincutScore = Math.max((int) Math.ceil(mincutScore * sizeFactor), 5);
        BiPartition biPartition = new BiPartition();
        MaxFlowMinCut maxFlowMinCut = createEdmondsKarp();
        List<Projection> projOrder = projector.calculateProjectionOrder(projections);
        //>> 遍历投影并对每个节点进行投影
        int i = 0;
        for (Projection proj : projOrder) {
            //仅尝试最佳投影
            if (i == CONSIDERED_PROJECTIONS)
                break;
            //>> 对投影后的节点排序
            maxFlowMinCut.setOrderedNodes(projections.get(proj));
            maxFlowMinCut.setNodeOrder();
            maxFlowMinCut.setMaxFlowLimit(mincutScore);
            maxFlowMinCut.reset();
            int cutScore = maxFlowMinCut.getMaxFlow();
            if (cutScore < mincutScore) {
                mincutScore = cutScore;
                biPartition = maxFlowMinCut.calcNodePartition();
            }
            i++;
        }
        return biPartition;
    }

    /**
     * 通过迭代分配位移后的 cellId 来保存多个单元（来自分离的非连通部分）
     *
     * @param cells     要存储的单元集合
     * @param newCellId 原始单元 ID
     */
    private void saveMultiCells(Set<IntHashSet> cells, int newCellId) {
        // 使用给定的 cellId 存储第一个单元。如果还有更多子单元需要存储，则创建 subcellId。
        Iterator<IntHashSet> iterator = cells.iterator();
        IntHashSet cell = iterator.next();
        for (IntCursor node : cell) {
            nodeToCellArr[node.value] = newCellId;
        }
        newCellId = newCellId << 1;

        while (iterator.hasNext()) {
            cell = iterator.next();
            for (IntCursor node : cell) {
                nodeToCellArr[node.value] = newCellId;
            }
            if (iterator.hasNext()) {
                cell = iterator.next();
                for (IntCursor node : cell) {
                    nodeToCellArr[node.value] = newCellId | 1;
                }
            }
            newCellId = newCellId << 1;
        }
    }

    /**
     * 递归调用 InertialFlow，以进一步对图的区域进行分区。
     * 可以同时对两个分区或仅对一个分区进一步分区。
     * 这是一次并行计算。当单元变得过小时，创建新线程的开销可能比在同一线程中以串行方式运行剩余 InertialFlow 更大。
     * 因此这里会对节点规模进行检查。
     *
     * @param invokeNext            需要进一步划分的分区
     * @param nodesPartition0       分区 0 的大小
     * @param nodesPartition1       分区 1 的大小
     * @param biPartitionProjection 投影的引用
     */
    private void recursion(boolean[] invokeNext, int nodesPartition0, int nodesPartition1, BiPartitionProjection biPartitionProjection) {
        int totalNodes = nodesPartition0 + nodesPartition1;
        for (int i : new int[]{0, 1}) {
            if (invokeNext[i]) {
                if (totalNodes > getMaxCellNodesNumber() * 4) {
                    executorService.execute(createInertialFlow(i, biPartitionProjection));
                } else {
                    InertialFlow inertialFlow = createInertialFlow(i, biPartitionProjection);
                    inertialFlow.run();
                }
            }
        }
    }

    /**
     * 创建新的 InertialFlow 对象
     *
     * @param partitionNumber       决定是第 0 个还是第 1 个分区
     * @param biPartitionProjection
     * @return
     */
    private InertialFlow createInertialFlow(int partitionNumber, BiPartitionProjection biPartitionProjection) {
        inverseSemaphore.beforeSubmit();
        InertialFlow inertialFlow = new InertialFlow();
        inertialFlow.setCellId(cellId << 1 | partitionNumber);
        inertialFlow.setNodeToCellArr(nodeToCellArr);
        inertialFlow.setGraph(graph);
        inertialFlow.setPartitioningData(pData);
        inertialFlow.setProjections(biPartitionProjection.getProjection(partitionNumber));
        inertialFlow.setEdgeFilter(edgeFilter);
        inertialFlow.setExecutorService(executorService);
        inertialFlow.setInverseSemaphore(inverseSemaphore);
        return inertialFlow;
    }

    /**
     * 判断是否应进一步拆分分区。
     * 如果不进一步拆分，则保存当前结果
     *
     * @param biPartition
     * @return 长度为 2 的布尔数组，决定哪些分区应被拆分
     */
    private boolean[] getInvokeNextOrSaveResult(BiPartition biPartition) {
        boolean[] invokeNext = new boolean[]{false, false};

        for (int i = 0; i < 2; i++) {
            boolean invokeRecursion = (cellId < MAX_SPLITTING_ITERATION) && (biPartition.getPartition(i).size() > getMaxCellNodesNumber());

            if ((cellId < MIN_SPLITTING_ITERATION))
                invokeRecursion = true;

            if (invokeRecursion)
                invokeNext[i] = true;

            else {
                Set<IntHashSet> cellsToStore = new HashSet<>();
                if (SEPARATEDISCONNECTED && cellId < MAX_SPLITTING_ITERATION)
                    cellsToStore = separateDisconnected(biPartition.getPartition(i));
                else
                    cellsToStore.add(biPartition.getPartition(i));
                saveMultiCells(cellsToStore, cellId << 1 | i);
            }
        }
        return invokeNext;
    }

    /**
     * 识别单元中非连通的部分，以便对它们进行拆分。
     * 当存在属于同一个单元但彼此之间没有实际边连接的节点（或节点组）时，这是必要的。
     * 此操作适用于因规模过小而无法再被常规 InertialFlow 拆分的单元。
     * 分离通过 dijkstra 边探索完成。
     *
     * @param nodeSet 要拆分的节点集合
     * @return 原始 nodeSet 的非连通子单元集合
     */
    public Set<IntHashSet> separateDisconnected(IntHashSet nodeSet) {
        Set<IntHashSet> disconnectedCells = new HashSet<>();
        EdgeExplorer edgeExplorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES);
        Queue<Integer> queue = new ArrayDeque<>();
        IntHashSet connectedCell = new IntHashSet(nodeSet.size());
        disconnectedCells.add(connectedCell);
        Iterator<IntCursor> iter;
        EdgeIterator edgeIterator;
        EdgeFilterSequence edgeFilterSequence = new EdgeFilterSequence();
        edgeFilterSequence.add(AccessFilter.allEdges((BooleanEncodedValue) encodedValue));
        if (edgeFilter != null)
            edgeFilterSequence.add(edgeFilter);

        while (!nodeSet.isEmpty()) {
            if (connectedCell.size() >= getMinCellNodesNumber() && disconnectedCells.size() < MAX_SUBCELL_NUMBER) {
                connectedCell = new IntHashSet();
                disconnectedCells.add(connectedCell);
            }
            iter = nodeSet.iterator();
            int startNode = iter.next().value;
            queue.offer(startNode);
            connectedCell.add(startNode);

            while (!queue.isEmpty()) {
                int currentNode = queue.poll();
                edgeIterator = edgeExplorer.setBaseNode(currentNode);

                while (edgeIterator.next()) {
                    int nextNode = edgeIterator.getAdjNode();
                    if (!edgeFilterSequence.accept(edgeIterator)
                            || connectedCell.contains(nextNode)
                            || !nodeSet.contains(nextNode))
                        continue;
                    queue.offer(nextNode);
                    connectedCell.add(nextNode);
                }
            }
            nodeSet.removeAll(connectedCell);
        }
        return disconnectedCells;
    }

    /**
     * 初始化最大流最小割算法。
     *
     * @return 最大流最小割
     */
    public MaxFlowMinCut createEdmondsKarp() {
        return new EdmondsKarpAStar(graph, pData, edgeFilter);
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public void setGraph(BaseGraph graph) {
        this.graph = graph;
    }

    public void setEdgeFilter(EdgeFilter edgeFilter) {
        this.edgeFilter = edgeFilter;
    }

    public void setPartitioningData(PartitioningData pData) {
        this.pData = pData;
    }

    public void setNodeToCellArr(int[] nodeToCellArr) {
        this.nodeToCellArr = nodeToCellArr;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setInverseSemaphore(InverseSemaphore inverseSemaphore) {
        this.inverseSemaphore = inverseSemaphore;
    }

    public void setEncodedValue(EncodedValue encodedValue) {
        this.encodedValue = encodedValue;
    }

    public void setProjections(Map<Projection, IntArrayList> projections) {
        this.projections = projections;
    }
}
