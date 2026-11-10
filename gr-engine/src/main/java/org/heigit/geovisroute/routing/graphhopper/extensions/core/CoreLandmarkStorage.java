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
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.predicates.IntObjectPredicate;
import com.carrotsearch.hppc.procedures.IntObjectProcedure;
import com.graphhopper.coll.MapEntry;
import com.graphhopper.routing.DijkstraBidirectionCHNoSOD;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.Subnetwork;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.lm.SplitArea;
import com.graphhopper.routing.subnetwork.SubnetworkStorage;
import com.graphhopper.routing.util.AreaIndex;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.*;
import com.graphhopper.util.Helper;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.LMEdgeFilterSequence;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GraphUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 存储核心区节点的地标距离
 * <p>
 * 本代码基于 GraphHopper GmbH 的代码。
 *
 * @author Peter Karich
 * @author Hendrik Leuschner
 * @author Andrzej Oles
 */
public class CoreLandmarkStorage extends LandmarkStorage {
    private static final long CORE_MAP_ROW_LENGTH = Integer.BYTES; // 4 bytes for the core node id

    private static final Logger logger = Logger.getLogger(CoreLandmarkStorage.class);
    private final RoutingCHGraphImpl core;
    private final LMEdgeFilterSequence landmarksFilter;
    private final DataAccess coreNodeIdDA;
    private final GRGraph graph;
    private final CoreLMConfig lmConfig;
    private IntHashSet subnetworkNodes;
    private EncodingManager encodingManager;

    public CoreLandmarkStorage(Directory dir, GRGraph graph, EncodingManager encodingManager,  final CoreLMConfig lmConfig, int landmarks) {
        this(dir, graph,encodingManager, graph.getCoreGraph(lmConfig.getSuperName()), lmConfig, landmarks);
    }

    //主要用于单元测试
    public CoreLandmarkStorage(Directory dir, GRGraph graph, EncodingManager encodingManager, RoutingCHGraph core, final CoreLMConfig lmConfig, int landmarks) {
        super(graph, encodingManager, dir, lmConfig, landmarks);
        this.graph = graph;
        this.encodingManager = encodingManager;
        this.lmConfig = lmConfig;
        this.core = (RoutingCHGraphImpl) core;
        this.landmarksFilter = lmConfig.getEdgeFilter();
        setMinimumNodes(Math.min(getBaseNodes() / 2, 10000));
        this.coreNodeIdDA = dir.create(getLandmarksFileName() + lmConfig.getName() + "_core_node_ids");
    }

    public void createCoreNodeIdDA() {
        int maxNode = GraphUtils.getBaseGraph(core).getNodes();

        // 初始化 DataAccess，预留足够空间存储所有核心区节点 id
        long maxBytes = maxNode * CORE_MAP_ROW_LENGTH;
        coreNodeIdDA.create(maxBytes);
        coreNodeIdDA.ensureCapacity(maxBytes);

        for (int index = 0, i = 0; i < maxNode; i++) {
            coreNodeIdDA.setInt(i * CORE_MAP_ROW_LENGTH, core.getLevel(i) >= maxNode ? index++ : 0);
        }
    }

    @Override
    public boolean loadExisting() {
        if (super.loadExisting()) {
            if (!coreNodeIdDA.loadExisting())
                throw new IllegalStateException("landmark weights loaded but not the node ID map, this is unexpected.");
            return true;
        }
        return false;
    }

    public String getLandmarksFileName() {
        return "landmarks_core_";
    }

    /**
     * 此方法计算地标以及到/从这些地标的初始权重。
     */
    public void createLandmarks() {
        if (isInitialized())
            throw new IllegalStateException("Initialize the landmark storage only once!");

        int minimumNodes = getMinimumNodes();
        int landmarks = getLandmarkCount();
        DataAccess landmarkWeightDA = getLandmarkWeightDA();
        List<int[]> landmarkIDs = getLandmarkIDs();
        AreaIndex<SplitArea> areaIndex = getAreaIndex();
        boolean logDetails = LOGGER.isDebugEnabled();
        SubnetworkStorage subnetworkStorage = getSubnetworkStorage();
        int coreNodes = getBaseNodes();

        // 用最大值填充 'from' 和 'to' 权重
        long maxBytes = (long) coreNodes * LM_ROW_LENGTH;
        landmarkWeightDA.create(2000);
        landmarkWeightDA.ensureCapacity(maxBytes);

        for (long pointer = 0; pointer < maxBytes; pointer += 2) {
            landmarkWeightDA.setShort(pointer, (short) SHORT_INFINITY);
        }

        int[] empty = new int[landmarks];
        Arrays.fill(empty, UNSET_SUBNETWORK);
        landmarkIDs.add(empty);

        byte[] subnetworks = new byte[coreNodes];
        Arrays.fill(subnetworks, (byte) UNSET_SUBNETWORK);

        String snKey = Subnetwork.key(lmConfig.getSuperName());
        // TODO 这里可以使用 EdgeBasedTarjanSCC 代替基于节点的 TarjanSCC 来直接得到小型网络，
        //  而不必使用 PrepareRoutingSubnetworks 中的 subnetworkEnc。
        if (!encodingManager.hasEncodedValue(snKey))
            throw new IllegalArgumentException("EncodedValue '" + snKey + "' does not exist. For Landmarks this is " +
                    "currently required (also used in PrepareRoutingSubnetworks). See #2256");

        // 排除之前在 PrepareRoutingSubnetworks 中标记的边，以避免出现 "connection not found" 之类的问题。
        final BooleanEncodedValue edgeInSubnetworkEnc = encodingManager.getBooleanEncodedValue(snKey);
        final IntHashSet blockedEdges;
        // 我们使用 areaIndex 将某些区域彼此分隔，但不永久修改基础图，
        // 以便其它算法仍能穿过这些区域进行路由。这样做是为了提高如欧亚大陆等区域的地标密度，
        // 从而提升查询速度。
        if (areaIndex != null) {
            StopWatch sw = new StopWatch().start();
            blockedEdges = findBorderEdgeIds(areaIndex);
            if (logDetails)
                logger.debug(configName() + "Made " + blockedEdges.size() + " edges inaccessible. Calculated country cut in " + sw.stop().getSeconds() + "s, " + Helper.getMemInfo());
        } else {
            blockedEdges = new IntHashSet();
        }

        EdgeFilter blockedEdgesFilter = edge -> !edge.get(edgeInSubnetworkEnc) && !blockedEdges.contains(edge.getEdge());
        EdgeFilter accessFilter = edge -> blockedEdgesFilter.accept(edge) && landmarksFilter.accept(edge);

        StopWatch sw = new StopWatch().start();
        TarjansCoreSCCAlgorithm tarjanAlgo = new TarjansCoreSCCAlgorithm(graph, core, accessFilter, false);
        List<IntArrayList> graphComponents = tarjanAlgo.findComponents();
        if (logDetails)
            logger.debug(configName() + "Calculated " + graphComponents.size() + " subnetworks via tarjan in " + sw.stop().getSeconds() + "s, " + Helper.getMemInfo());

        createCoreNodeIdDA();
        String additionalInfo = "";
        // 估算 factor
        if (getFactor() <= 0) {
            // 需要 'factor' 才能仅用一个 short 值存储权重而又不损失过多精度。
            // 这个 factor 相当难以选取，我们通过一些“测试地标”的探索来估算它，
            // 参见 estimateMaxWeight。如果对小区域取的距离过大，会导致（略微）次优的路线，
            // 因为会有过大的舍入误差。但取值过小又不利于性能，
            // 例如对德国来说至少 1500km 非常重要，否则速度至少慢一倍（例如对 1000km 而言）
            double maxWeight = estimateMaxWeight(graphComponents, accessFilter);
            setMaximumWeight(maxWeight);
            additionalInfo = ", maxWeight:" + maxWeight + " from quick estimation";
        }
        double factor = getFactor();

        if (logDetails)
            logger.debug(configName() + "init landmarks for subnetworks with node count greater than " + minimumNodes + " with factor:" + factor + additionalInfo);

        int nodes = 0;
        for (IntArrayList subnetworkIds : graphComponents) {
            nodes += subnetworkIds.size();
            if (subnetworkIds.size() < minimumNodes)
                continue;
            if (factor <= 0)
                throw new IllegalStateException("factor wasn't initialized " + factor + ", subnetworks:"
                        + graphComponents.size() + ", minimumNodes:" + minimumNodes + ", current size:" + subnetworkIds.size());

            subnetworkNodes = new IntHashSet(subnetworkIds);
            int index = subnetworkIds.size() - 1;
            for (; index >= 0; index--) {
                int nextStartNode = subnetworkIds.get(index);
                if (subnetworks[getIndex(nextStartNode)] == UNSET_SUBNETWORK) {
                    if (logDetails) {
                        GHPoint p = createPoint(graph, nextStartNode);
                        logger.debug(configName() + "start node: " + nextStartNode + " (" + p + ") subnetwork " + index + ", subnetwork size: " + subnetworkIds.size()
                                + ", " + Helper.getMemInfo() + ((areaIndex == null) ? "" : " area:" + areaIndex.query(p.lat, p.lon)));
                    }
                    if (createLandmarksForSubnetwork(nextStartNode, subnetworks, accessFilter))
                        break;
                }
            }
            if (index < 0)
                logger.warn("next start node not found in big enough network of size " + subnetworkIds.size() + ", first element is " + subnetworkIds.get(0) + ", " + createPoint(graph, subnetworkIds.get(0)));
        }

        int subnetworkCount = landmarkIDs.size();
        // 存储所有地标节点 ID 以及一个表示 factor 本身的 int。
        landmarkWeightDA.ensureCapacity(maxBytes /* 地标权重 */ + (long) subnetworkCount * landmarks /* 每个子网络的地标映射 */);

        // 计算指向地标映射的偏移量
        long bytePos = maxBytes;
        for (int[] lms : landmarkIDs) {
            for (int lmNodeId : lms) {
                landmarkWeightDA.setInt(bytePos, lmNodeId);
                bytePos += 4L;
            }
        }

        landmarkWeightDA.setHeader(0, coreNodes);
        landmarkWeightDA.setHeader(4, landmarks);
        landmarkWeightDA.setHeader(2 * 4, subnetworkCount);
        if (factor * DOUBLE_MLTPL > Integer.MAX_VALUE)
            throw new UnsupportedOperationException("landmark weight factor cannot be bigger than Integer.MAX_VALUE " + factor * DOUBLE_MLTPL);
        landmarkWeightDA.setHeader(3 * 4, (int) Math.round(factor * DOUBLE_MLTPL));

        // 将快速的 byte[] 序列化到 DataAccess 中
        subnetworkStorage.create(coreNodes);
        for (int nodeId = 0; nodeId < subnetworks.length; nodeId++) {
            subnetworkStorage.setSubnetwork(nodeId, subnetworks[nodeId]);
        }

        if (logDetails)
            logger.debug(configName() + "Finished landmark creation. Subnetwork node count sum " + nodes + " vs. nodes " + coreNodes);
        setInitialized(true);
    }

    private String configName() {
        return "[" + lmConfig.getName() + "] ";
    }

    public int getIndex(int node) {
        return coreNodeIdDA.getInt(node * CORE_MAP_ROW_LENGTH);
    }

    protected int getBaseNodes() {
        return core.getCoreNodes();
    }

    protected static class CoreEdgeFilter implements CHEdgeFilter {
        private final RoutingCHGraph graph;
        EdgeFilter edgeFilter;
        private final int coreNodeLevel;

        public CoreEdgeFilter(RoutingCHGraph graph, EdgeFilter edgeFilter) {
            this.graph = graph;
            this.edgeFilter = edgeFilter;
            coreNodeLevel = GraphUtils.getBaseGraph(graph).getNodes();
        }

        @Override
        public boolean accept(RoutingCHEdgeIteratorState edgeState) {
            if (isCoreEdge(edgeState))
                return acceptEdge(edgeState);
            else
                return false;
        }

        private boolean isCoreEdge(RoutingCHEdgeIteratorState edgeState) {
            int base = edgeState.getBaseNode();
            int adj = edgeState.getAdjNode();

            return graph.getLevel(base) >= coreNodeLevel && graph.getLevel(adj) >= coreNodeLevel;
        }

        private boolean acceptEdge(RoutingCHEdgeIteratorState edgeState) {
            if (edgeFilter == null)
                return true;
            if (edgeState.isShortcut())
                return true;

            return edgeFilter.accept(((RoutingCHEdgeIteratorStateImpl) edgeState).getBaseGraphEdgeState());
        }
    }

    @Override
    public LandmarkExplorer getLandmarkExplorer(EdgeFilter accessFilter, Weighting weighting, boolean reverse) {
        return new CoreLandmarkExplorer(core, accessFilter, reverse, this.subnetworkNodes);
    }

    @Override
    public LandmarkExplorer getLandmarkSelector(EdgeFilter accessFilter) {
        return new CoreLandmarkSelector(core, accessFilter, false, this.subnetworkNodes);
    }

    /**
     * 该类用于计算地标位置（均匀分布）。
     * 它继承自 DijkstraBidirectionRef，但仅作为前向或后向搜索使用。
     */
    private class CoreLandmarkExplorer extends DijkstraBidirectionCHNoSOD implements LandmarkExplorer {
        private final boolean reverse;
        private SPTEntry lastEntry;

        public CoreLandmarkExplorer(RoutingCHGraph g, EdgeFilter accessFilter, boolean reverse, IntHashSet subnetworkNodes) {
            super(g);
            //TODO: 针对从强连通分量外部选取节点的问题实现更好的解决方案。
            // 在边过滤器配置正确并按预期工作的情况下，原则上不应需要额外的检查。
            CHEdgeFilter subnetworkFilter = edge -> subnetworkNodes == null || subnetworkNodes.contains(edge.getAdjNode());
            CHEdgeFilter coreEdgeFilter = new CoreEdgeFilter(g, accessFilter);
            this.levelEdgeFilter = edge -> subnetworkFilter.accept(edge) && coreEdgeFilter.accept(edge);
            this.reverse = reverse;
            // 将两个方向之一标记为已完成
            if (reverse)
                finishedFrom = true;
            else
                finishedTo = true;

            // 不应计算任何路径
            setUpdateBestPath(false);
        }

        @Override
        public void setStartNode(int startNode) {
            if (reverse)
                initTo(startNode, 0);
            else
                initFrom(startNode, 0);
        }

        @Override
        public int getFromCount() {
            return bestWeightMapFrom.size();
        }

        @Override
        public void runAlgo() {
            super.runAlgo();
        }

        // 需要重写 DijkstraBidirectionCHNoSOD 的该方法，因为它使用图的 Weighting 而非 CoreLandmarkStorage 的 Weighting。
        // 不过该图使用的是基于转向代价的 Weighting，而这不允许用于 LM 距离计算。
        @Override
        protected double calcWeight(RoutingCHEdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
            return edgeState.getWeight(reverse);
        }

        @Override
        public SPTEntry getLastEntry() {
            if (!finished())
                throw new IllegalStateException("Cannot get max weight if not yet finished");
            return lastEntry;
        }

        @Override
        public boolean finished() {
            if (reverse) {
                lastEntry = currTo;
                return finishedTo;
            } else {
                lastEntry = currFrom;
                return finishedFrom;
            }
        }

        @Override
        public boolean setSubnetworks(final byte[] subnetworks, final int subnetworkId) {
            if (subnetworkId > 127)
                throw new IllegalStateException("Too many subnetworks " + subnetworkId);

            final AtomicBoolean failed = new AtomicBoolean(false);
            IntObjectMap<SPTEntry> map = reverse ? bestWeightMapTo : bestWeightMapFrom;
            map.forEach((IntObjectPredicate<SPTEntry>) (nodeId, value) -> {
                nodeId = getIndex(nodeId);
                int sn = subnetworks[nodeId];
                if (sn != subnetworkId) {
                    if (sn != UNSET_SUBNETWORK && sn != UNCLEAR_SUBNETWORK) {
                        // 这样写不好看，但在现实中可能发生，参见 testWithOnewaySubnetworks
                        logger.error("subnetworkId for node " + nodeId
                                + " (" + createPoint(graph.getBaseGraph(), nodeId) + ") already set (" + sn + "). " + "Cannot change to " + subnetworkId);

                        failed.set(true);
                        return false;
                    }

                    subnetworks[nodeId] = (byte) subnetworkId;
                }
                return true;
            });
            return failed.get();
        }

        @Override
        public void initLandmarkWeights(final int lmIdx, int lmNodeId, final long rowSize, final int offset) {
            IntObjectMap<SPTEntry> map = reverse ? bestWeightMapTo : bestWeightMapFrom;
            final AtomicInteger maxedout = new AtomicInteger(0);
            final Map.Entry<Double, Double> finalMaxWeight = new MapEntry<>(0d, 0d);

            map.forEach((IntObjectProcedure<SPTEntry>) (nodeId, b) -> {
                nodeId = getIndex(nodeId);
                if (!setWeight(nodeId * rowSize + lmIdx * 4L + offset, b.weight)) {
                    maxedout.incrementAndGet();
                    finalMaxWeight.setValue(Math.max(b.weight, finalMaxWeight.getValue()));
                }
            });

            if ((double) maxedout.get() / map.size() > 0.1) {
                logger.warn("landmark " + lmIdx + " (" + nodeAccess.getLat(lmNodeId) + "," + nodeAccess.getLon(lmNodeId) + "): " +
                        "too many weights were maxed out (" + maxedout.get() + "/" + map.size() + "). Use a bigger factor than " + getFactor()
                        + ". For example use maximum_lm_weight: " + finalMaxWeight.getValue() * 1.2 + " in your LM profile definition");
            }
        }
    }

    private class CoreLandmarkSelector extends CoreLandmarkExplorer {

        public CoreLandmarkSelector(RoutingCHGraph g, EdgeFilter accessFilter, boolean reverse, IntHashSet subnetworkNodes) {
            super(g, accessFilter, reverse, subnetworkNodes);
        }

        @Override
        protected double calcWeight(RoutingCHEdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
            if (edgeState.isShortcut())
                return expandEdge(edgeState);

            if (super.calcWeight(edgeState, reverse, prevOrNextEdgeId) >= Double.MAX_VALUE)
                return Double.POSITIVE_INFINITY;

            return 1;
        }

        private int expandEdge(RoutingCHEdgeIteratorState mainEdgeState) {
            if (!mainEdgeState.isShortcut())
                return 1;

            int skippedEdge1 = mainEdgeState.getSkippedEdge1();
            int skippedEdge2 = mainEdgeState.getSkippedEdge2();
            int from = mainEdgeState.getBaseNode();
            int to = mainEdgeState.getAdjNode();

            RoutingCHEdgeIteratorState iter1, iter2;
            iter1 = core.getEdgeIteratorState(skippedEdge1, from);
            if (iter1 == null) {
                iter1 = core.getEdgeIteratorState(skippedEdge2, from);
                iter2 = core.getEdgeIteratorState(skippedEdge1, to);
            } else {
                iter2 = core.getEdgeIteratorState(skippedEdge2, to);
            }

            return expandEdge(iter1) + expandEdge(iter2);
        }

    }

    @Override
    public void flush() {
        super.flush();
        coreNodeIdDA.flush();
    }

    @Override
    public void close() {
        super.close();
        coreNodeIdDA.close();
    }
}
