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
package org.heigit.geovisroute.matrix.algorithms.core;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectMap;
import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.RoutingCHEdgeExplorer;
import com.graphhopper.storage.RoutingCHEdgeIterator;
import com.graphhopper.storage.RoutingCHEdgeIteratorState;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.util.EdgeIterator;
import org.heigit.geovisroute.matrix.*;
import org.heigit.geovisroute.matrix.algorithms.AbstractContractedMatrixAlgorithm;
import org.heigit.geovisroute.matrix.algorithms.dijkstra.DijkstraManyToMany;
import org.heigit.geovisroute.routing.algorithms.SubGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.core.CoreDijkstraFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.core.CoreMatrixFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.AveragedMultiTreeSPEntry;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.MultiTreeSPEntryItem;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GraphUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.heigit.geovisroute.matrix.util.GraphUtils.isCoreNode;

/**
 * 一种基于 Core 和 Dijkstra 的算法，计算从多个起点到多个终点节点的权重。
 * 使用 core 和真正的多对多（many to many）。
 *
 * @author Hendrik Leuschner
 */
public class CoreMatrixAlgorithm extends AbstractContractedMatrixAlgorithm {
    protected int coreNodeLevel;
    protected int nodeCount;
    private int treeEntrySize;
    private boolean hasTurnWeighting = false;
    private boolean swap = false;

    private PriorityQueue<AveragedMultiTreeSPEntry> upwardQueue;
    private IntHashSet coreEntryPoints;
    private IntHashSet coreExitPoints;
    private IntObjectMap<AveragedMultiTreeSPEntry> bestWeightMap;
    private IntObjectMap<List<AveragedMultiTreeSPEntry>> bestWeightMapCore;
    private IntObjectMap<AveragedMultiTreeSPEntry> targetMap;
    private IntHashSet targetSet;
    private MultiTreeMetricsExtractor pathMetricsExtractor;
    private CoreDijkstraFilter additionalCoreEdgeFilter;
    private SubGraph targetGraph;
    private boolean hasInfiniteUTurnCosts;


    @Override
    public void init(MatrixRequest req, GraphHopper gh, RoutingCHGraph chGraph, String encodeName, Weighting weighting) {
        Weighting preparedWeighting = chGraph.getWeighting();
        super.init(req, gh, chGraph, encodeName, preparedWeighting);
        hasTurnWeighting = preparedWeighting.hasTurnCosts();
        nodeCount = GraphUtils.getBaseGraph(chGraph).getNodes();
        coreNodeLevel = nodeCount;
        pathMetricsExtractor = new MultiTreeMetricsExtractor(req.getMetrics(), chGraph, encodeName, preparedWeighting, req.getUnits());
        additionalCoreEdgeFilter = new CoreMatrixFilter(chGraph);
        initCollections(10);
        hasInfiniteUTurnCosts = req.hasInfiniteUTurnCosts();
    }

    public void init(MatrixRequest req, GraphHopper gh, RoutingCHGraph chGraph, String encodeName, Weighting weighting, EdgeFilter additionalEdgeFilter) {
        this.init(req, gh, chGraph, encodeName, weighting);
        if (additionalEdgeFilter != null)
            additionalCoreEdgeFilter.addRestrictionFilter(additionalEdgeFilter);
    }

    public void init(MatrixRequest req, RoutingCHGraph chGraph, String encodeName, Weighting weighting, EdgeFilter additionalEdgeFilter) {
        this.init(req, null, chGraph, encodeName, weighting, additionalEdgeFilter);
    }

    protected void initCollections(int size) {
        upwardQueue = new PriorityQueue<>(size);
        coreEntryPoints = new IntHashSet(size);
        coreExitPoints = new IntHashSet(size);
        targetSet = new IntHashSet(size);
        bestWeightMap = new GHIntObjectHashMap<>(size);
        bestWeightMapCore = new GHIntObjectHashMap<>(size);
        targetMap = new GHIntObjectHashMap<>(size);
    }

    @Override
    /**
     * 根据给定的指标计算从 srcData 到 dstData 的 MatrixResult
     */
    public MatrixResult compute(MatrixLocations srcData, MatrixLocations dstData, int metrics) throws Exception {
        // 当 dstData.size > srcData.size 时搜索效率更高，因此检查是否需要交换二者
        swap = checkSwapSrcDst(srcData, dstData);
        if (swap) {
            MatrixLocations tmp = srcData;
            srcData = dstData;
            dstData = tmp;
        }
        this.treeEntrySize = srcData.size();

        TargetGraphBuilder.TargetGraphResults targetGraphResults = new TargetGraphBuilder().prepareTargetGraph(dstData.getNodeIds(), chGraph, swap, coreNodeLevel);
        targetGraph = targetGraphResults.getTargetGraph();
        coreExitPoints.addAll(targetGraphResults.getCoreExitPoints());

        targetSet.addAll(dstData.getNodeIds());

        float[] times = null;
        float[] distances = null;
        float[] weights = null;

        int tableSize = srcData.size() * dstData.size();
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DURATION))
            times = new float[tableSize];
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DISTANCE))
            distances = new float[tableSize];
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.WEIGHT))
            weights = new float[tableSize];

        if (!isValid(srcData, dstData)) {
            for (int srcIndex = 0; srcIndex < srcData.size(); srcIndex++)
                pathMetricsExtractor.setEmptyValues(srcIndex, dstData, times, distances, weights);
        } else {
            this.additionalCoreEdgeFilter.setInCore(false);
            runPhaseOutsideCore(srcData);

            this.additionalCoreEdgeFilter.setInCore(true);
            runPhaseInsideCore();

            extractMetrics(srcData, dstData, times, distances, weights);
        }

        if (swap) {
            MatrixLocations tmp = srcData;
            srcData = dstData;
            dstData = tmp;
            float[][] results = swapResults(srcData, dstData, times, distances, weights);
            times = results[0];
            distances = results[1];
            weights = results[2];
        }

        MatrixResult mtxResult = new MatrixResult(srcData.getLocations(), dstData.getLocations());

        setTables(metrics, times, distances, weights, mtxResult);

        return mtxResult;
    }

    private void setTables(int metrics, float[] times, float[] distances, float[] weights, MatrixResult mtxResult) {
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DURATION))
            mtxResult.setTable(MatrixMetricsType.DURATION, times);
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DISTANCE))
            mtxResult.setTable(MatrixMetricsType.DISTANCE, distances);
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.WEIGHT))
            mtxResult.setTable(MatrixMetricsType.WEIGHT, weights);
    }

    /**
     * /
     * /
     * __________OUT-CORE
     * /
     * /
     **/
    private void runPhaseOutsideCore(MatrixLocations srcData) {
        prepareSourceNodes(srcData.getNodeIds());
        boolean finishedFrom = false;
        RoutingCHEdgeExplorer upAndCoreExplorer = swap ? chGraph.createInEdgeExplorer()
                : chGraph.createOutEdgeExplorer();
        while (!finishedFrom && !isMaxVisitedNodesExceeded()) {
            finishedFrom = !fillEdgesOutsideCore(upAndCoreExplorer);
        }
    }

    /**
     * 将源节点添加到队列和映射中
     *
     * @param from 源节点 ID 数组
     */
    private void prepareSourceNodes(int[] from) {
        for (int i = 0; i < from.length; i++) {
            if (from[i] == -1)
                continue;
            //如果两个查询点位于同一节点上，可能会出现这种情况
            AveragedMultiTreeSPEntry existing = bestWeightMap.getOrDefault(from[i], null);
            if (existing != null) {
                existing.getItem(i).setWeight(0.0);
                upwardQueue.remove(existing);
                existing.updateWeights();
                upwardQueue.add(existing);
                continue;
            }

            AveragedMultiTreeSPEntry newFrom = new AveragedMultiTreeSPEntry(from[i], EdgeIterator.NO_EDGE, 0.0, true, null, from.length);
            newFrom.setSubItemOriginalEdgeIds(EdgeIterator.NO_EDGE);

            newFrom.getItem(i).setWeight(0.0);
            newFrom.updateWeights();
            upwardQueue.add(newFrom);

            bestWeightMap.put(from[i], newFrom);
            updateTarget(newFrom);
        }
    }

    /**
     * 从源节点搜索至 core 入口点
     *
     * @return 队列为空时返回 false
     */
    public boolean fillEdgesOutsideCore(RoutingCHEdgeExplorer upAndCoreExplorer) {
        if (upwardQueue.isEmpty())
            return false;

        AveragedMultiTreeSPEntry currFrom = upwardQueue.poll();

        if (isCoreNode(chGraph, currFrom.getAdjNode(), nodeCount, coreNodeLevel)) {
            // core 入口点，不松弛其边
            coreEntryPoints.add(currFrom.getAdjNode());
            // 对于常规 CH Dijkstra，我们并不期望存在条目，因为被选取的节点应已确定
            if (considerTurnRestrictions()) {
                List<AveragedMultiTreeSPEntry> existingEntryList = bestWeightMapCore.get(currFrom.getAdjNode());
                if (existingEntryList == null)
                    initBestWeightMapEntryList(bestWeightMapCore, currFrom.getAdjNode()).add(currFrom);
                else
                    existingEntryList.add(currFrom);
            }
        } else
            fillEdgesUpward(currFrom, upwardQueue, bestWeightMap, upAndCoreExplorer);


        visitedNodes++;

        return true;
    }

    List<AveragedMultiTreeSPEntry> initBestWeightMapEntryList(IntObjectMap<List<AveragedMultiTreeSPEntry>> bestWeightMap, int traversalId) {
        if (bestWeightMap.get(traversalId) != null)
            throw new IllegalStateException("Core entry point already exists in best weight map.");

        List<AveragedMultiTreeSPEntry> entryList = new ArrayList<>(5);
        bestWeightMap.put(traversalId, entryList);

        return entryList;
    }

    /**
     * 为向上搜索遍历与 currEdge 相邻的所有边。不搜索 core。
     *
     * @param currEdge      当前边
     * @param prioQueue     用于添加新条目的队列
     * @param bestWeightMap 用于添加新条目的映射
     * @param explorer      用于向上搜索的边探索器
     */
    void fillEdgesUpward(AveragedMultiTreeSPEntry currEdge, PriorityQueue<AveragedMultiTreeSPEntry> prioQueue, IntObjectMap<AveragedMultiTreeSPEntry> bestWeightMap,
                         RoutingCHEdgeExplorer explorer) {
        RoutingCHEdgeIterator iter = explorer.setBaseNode(currEdge.getAdjNode());
        while (iter.next()) {
            AveragedMultiTreeSPEntry entry = bestWeightMap.get(iter.getAdjNode());

            if (entry == null) {
                entry = new AveragedMultiTreeSPEntry(iter.getAdjNode(), iter.getEdge(), Double.POSITIVE_INFINITY, true, null, currEdge.getSize());
                boolean addToQueue = iterateMultiTree(currEdge, iter, entry);
                if (addToQueue) {
                    entry.updateWeights();
                    bestWeightMap.put(iter.getAdjNode(), entry);
                    prioQueue.add(entry);
                    updateTarget(entry);
                }
            } else {
                boolean addToQueue = iterateMultiTree(currEdge, iter, entry);
                if (addToQueue) {
                    prioQueue.remove(entry);
                    entry.updateWeights();
                    prioQueue.add(entry);
                    updateTarget(entry);
                }
            }
        }
        if (!targetGraph.containsNode(currEdge.getAdjNode())) currEdge.resetUpdate(false);
    }

    /**
     * 遍历一个 MultiTree 条目及其子项以调整新权重
     *
     * @param currEdge 当前基础边
     * @param iter     与 currEdge 相邻的迭代器
     * @param adjEntry 属于 iter 的条目
     * @return 若任一权重发生更新则返回 true
     */
    private boolean iterateMultiTree(AveragedMultiTreeSPEntry currEdge, RoutingCHEdgeIterator iter, AveragedMultiTreeSPEntry adjEntry) {
        boolean addToQueue = false;
        for (int i = 0; i < treeEntrySize; ++i) {
            MultiTreeSPEntryItem currEdgeItem = currEdge.getItem(i);
            double entryWeight = currEdgeItem.getWeight();

            if (entryWeight == Double.POSITIVE_INFINITY)
                continue;
            double edgeWeight;

            if (!additionalCoreEdgeFilter.accept(iter)) {
                continue;
            }

            edgeWeight = calcWeight(iter, swap, currEdgeItem.getOriginalEdge());
            if (Double.isInfinite(edgeWeight))
                continue;
            double tmpWeight = edgeWeight + entryWeight;

            MultiTreeSPEntryItem eeItem = adjEntry.getItem(i);
            if (eeItem.getWeight() > tmpWeight) {
                eeItem.setWeight(tmpWeight);
                eeItem.setEdge(iter.getEdge());
                eeItem.setOriginalEdge(iter.getOrigEdge());
                eeItem.setParent(currEdge);
                eeItem.setUpdate(true);
                addToQueue = true;
            }
        }

        return addToQueue;
    }

    /**
     * 根据更新条目更新 targetMap 中的目标条目。这有必要将目标结果与正在进行的计算区分开
     *
     * @param update 应更新目标权重的新条目
     */
    private void updateTarget(AveragedMultiTreeSPEntry update) {
        int nodeId = update.getAdjNode();
        if (targetSet.contains(nodeId)) {
            if (!targetMap.containsKey(nodeId)) {
                AveragedMultiTreeSPEntry newTarget = new AveragedMultiTreeSPEntry(nodeId, EdgeIterator.NO_EDGE, Double.POSITIVE_INFINITY, true, null, update.getSize());
                newTarget.setSubItemOriginalEdgeIds(EdgeIterator.NO_EDGE);
                targetMap.put(nodeId, newTarget);
            }
            AveragedMultiTreeSPEntry target = targetMap.get(nodeId);
            for (int i = 0; i < treeEntrySize; ++i) {
                MultiTreeSPEntryItem targetItem = target.getItem(i);
                double targetWeight = targetItem.getWeight();

                MultiTreeSPEntryItem msptSubItem = update.getItem(i);
                double updateWeight = msptSubItem.getWeight();

                if (targetWeight > updateWeight) {
                    targetItem.setWeight(updateWeight);
                    targetItem.setEdge(msptSubItem.getEdge());
                    targetItem.setOriginalEdge(msptSubItem.getOriginalEdge());
                    targetItem.setParent(msptSubItem.getParent());
                }
            }
        }
    }

    /**
     * /
     * /
     * __________IN-CORE
     * /
     * /
     **/

    /**
     * 为 core 和向下阶段创建多对多 Dijkstra 并运行
     */
    private void runPhaseInsideCore() {
        // 仅在 core 内部计算所有路径
        DijkstraManyToMany algorithm = new DijkstraManyToMany(chGraph, bestWeightMap, bestWeightMapCore, weighting, TraversalMode.NODE_BASED);
        algorithm.setInfiniteUTurnCost(this.hasInfiniteUTurnCosts);

        algorithm.setEdgeFilter(this.additionalCoreEdgeFilter);
        algorithm.setTreeEntrySize(this.treeEntrySize);
        algorithm.setHasTurnWeighting(this.hasTurnWeighting);
        algorithm.setMaxVisitedNodes(this.maxVisitedNodes);
        algorithm.setVisitedNodes(this.visitedNodes);
        algorithm.setTargetGraphExplorer(targetGraph.createExplorer());
        algorithm.setTargetMap(this.targetMap);
        algorithm.setTargetSet(this.targetSet);
        algorithm.setSwap(this.swap);

        int[] entryPoints = coreEntryPoints.toArray();
        int[] exitPoints = coreExitPoints.toArray();
        algorithm.calcPaths(entryPoints, exitPoints);
        visitedNodes = algorithm.getVisitedNodes();
    }

    /**
     * /
     * /
     * __________UTIL
     * /
     * /
     **/

    private boolean isValid(MatrixLocations srcData, MatrixLocations dstData) {
        return !(!srcData.hasValidNodes() || !dstData.hasValidNodes());
    }

    /**
     * 当源数量少、目标数量多时，搜索比反过来更高效。
     * 如果源多于目标，则将其交换，并且所有计算都以反向方式进行。
     * 最终结果会被还原交换以返回正确结果。
     *
     * @param srcData 原始源数据
     * @param dstData 原始目标数据
     * @return
     */
    private boolean checkSwapSrcDst(MatrixLocations srcData, MatrixLocations dstData) {
        return (srcData.size() > dstData.size());
    }

    /**
     * 在 src 与 dst 被交换的情况下，反转结果矩阵（以扁平数组表示）
     *
     * @param srcData   原始未交换的源数据
     * @param dstData   原始未交换的目标数据
     * @param times     已交换的结果数组
     * @param distances 已交换的结果数组
     * @param weights   已交换的结果数组
     * @return 未交换的结果数组 [times, distances, weights]
     */
    private float[][] swapResults(MatrixLocations srcData, MatrixLocations dstData, float[] times, float[] distances, float[] weights) {
        boolean hasTimes = times != null;
        boolean hasDistances = distances != null;
        boolean hasWeights = weights != null;
        float[] newTimes = new float[hasTimes ? times.length : 0];
        float[] newDistances = new float[hasDistances ? distances.length : 0];
        float[] newWeights = new float[hasWeights ? weights.length : 0];

        int i = 0;
        int srcSize = srcData.size();
        int dstSize = dstData.size();
        for (int dst = 0; dst < dstSize; dst++) {
            for (int src = 0; src < srcSize; src++) {
                int index = dst + src * dstSize;
                if (hasTimes)
                    newTimes[index] = times[i];
                if (hasDistances)
                    newDistances[index] = distances[i];
                if (hasWeights)
                    newWeights[index] = weights[i];
                i++;
            }
        }
        return new float[][]{newTimes, newDistances, newWeights};
    }

    private void extractMetrics(MatrixLocations srcData, MatrixLocations dstData, float[] times, float[] distances, float[] weights) throws Exception {
        AveragedMultiTreeSPEntry[] destTrees = new AveragedMultiTreeSPEntry[dstData.size()];
        for (int i = 0; i < dstData.size(); i++)
            destTrees[i] = targetMap.get(dstData.getNodeIds()[i]);

        AveragedMultiTreeSPEntry[] originalDestTrees = new AveragedMultiTreeSPEntry[dstData.size()];

        int j = 0;
        for (int i = 0; i < dstData.size(); i++) {
            if (dstData.getNodeIds()[i] != -1) {
                originalDestTrees[i] = destTrees[j];
                ++j;
            } else {
                originalDestTrees[i] = null;
            }
        }
        pathMetricsExtractor.setSwap(swap);
        pathMetricsExtractor.calcValues(originalDestTrees, srcData, dstData, times, distances, weights);
    }

    boolean considerTurnRestrictions() {
        return hasTurnWeighting;
    }

    public void setMaxVisitedNodes(int numberOfNodes) {
        this.maxVisitedNodes = numberOfNodes;
    }

    double calcPathWeight(RoutingCHEdgeIteratorState iter, SPTEntry currEdge, boolean reverse) {
        return calcWeight(iter, reverse, currEdge.originalEdge) + currEdge.getWeightOfVisitedPath();
    }

    // TODO 重构：检查为何所有边权重计算相关逻辑需要放在此算法中，而不是放在权重或迭代器中
    double calcWeight(RoutingCHEdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        double edgeWeight = edgeState.getWeight(reverse);
        double turnCost = getTurnWeight(prevOrNextEdgeId, edgeState.getBaseNode(), edgeState.getOrigEdge(), reverse);
        return edgeWeight + turnCost;
    }

    double getTurnWeight(int edgeA, int viaNode, int edgeB, boolean reverse) {
        return reverse
                ? chGraph.getTurnWeight(edgeB, viaNode, edgeA)
                : chGraph.getTurnWeight(edgeA, viaNode, edgeB);
    }

    long calcTime(RoutingCHEdgeIteratorState iter, SPTEntry currEdge, boolean reverse) {
        return 0;
    }
}
