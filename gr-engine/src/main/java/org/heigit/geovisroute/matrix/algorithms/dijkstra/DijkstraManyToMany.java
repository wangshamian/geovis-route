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
package org.heigit.geovisroute.matrix.algorithms.dijkstra;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.RoutingCHEdgeExplorer;
import com.graphhopper.storage.RoutingCHEdgeIterator;
import com.graphhopper.storage.RoutingCHEdgeIteratorState;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.Parameters;
import org.heigit.geovisroute.routing.algorithms.AbstractManyToManyRoutingAlgorithm;
import org.heigit.geovisroute.routing.algorithms.SubGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.AveragedMultiTreeSPEntry;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.MultiTreeSPEntryItem;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GraphUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.MultiSourceStoppingCriterion;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;

import static org.heigit.geovisroute.matrix.util.GraphUtils.isCoreNode;

/**
 * 一种基于 Core 和 Dijkstra 的算法，在 core 内部及向下方向执行多对多搜索。
 * 只能作为 core 矩阵算法的一部分使用。
 *
 * @author Hendrik Leuschner
 */

public class DijkstraManyToMany extends AbstractManyToManyRoutingAlgorithm {
    protected IntObjectMap<AveragedMultiTreeSPEntry> bestWeightMap;
    protected PriorityQueue<AveragedMultiTreeSPEntry> prioQueue;
    protected AveragedMultiTreeSPEntry currEdge;
    IntObjectMap<List<AveragedMultiTreeSPEntry>> bestWeightMapCore;
    IntObjectMap<AveragedMultiTreeSPEntry> targetMap;
    IntHashSet targetSet;
    private final RoutingCHGraph chGraph;
    private IntHashSet coreExitPoints;
    private RoutingCHEdgeExplorer targetGraphExplorer;
    private MultiSourceStoppingCriterion stoppingCriterion;
    private int visitedNodes;
    private int treeEntrySize;
    private boolean hasTurnWeighting = false;
    private final int coreNodeLevel;
    private final int nodeCount;
    private boolean swap = false;

    public DijkstraManyToMany(RoutingCHGraph chGraph, Weighting weighting, TraversalMode tMode) {
        super(chGraph, weighting, tMode);
        this.chGraph = chGraph;
        this.coreNodeLevel = GraphUtils.getBaseGraph(chGraph).getNodes();
        this.nodeCount = chGraph.getNodes();
        int size = Math.min(Math.max(200, chGraph.getNodes() / 10), 2000);
        initCollections(size);
    }

    public DijkstraManyToMany(RoutingCHGraph chGraph, IntObjectMap<AveragedMultiTreeSPEntry> existingWeightMap, IntObjectMap<List<AveragedMultiTreeSPEntry>> existingCoreWeightMap, Weighting weighting, TraversalMode tMode) {
        this(chGraph, weighting, tMode);
        bestWeightMap = existingWeightMap;
        bestWeightMapCore = existingCoreWeightMap;
    }

    protected void initCollections(int size) {
        prioQueue = new PriorityQueue<>(size);
        bestWeightMap = new GHIntObjectHashMap<>(size);
    }

    public void reset() {
        prioQueue.clear();
        bestWeightMap.clear();
    }

    /**
     * 根据 from[] 创建 coreExitPoints，我们需要它来启动向下搜索
     *
     * @param from
     * @param coreExitPoints
     */
    public void prepare(int[] from, int[] coreExitPoints) {
        int targetsCount = coreExitPoints.length;
        this.coreExitPoints = new IntHashSet(targetsCount);

        for (int i = 0; i < coreExitPoints.length; ++i) {
            int nodeId = coreExitPoints[i];
            if (nodeId >= 0) {
                this.coreExitPoints.add(nodeId);
            }
        }
    }

    public AveragedMultiTreeSPEntry[] calcPaths(int[] from, int[] to) {
        if (from == null || to == null)
            throw new IllegalArgumentException("Input points are null");

        prepare(from, to);
        addEntriesFromMapToQueue();
        outEdgeExplorer = swap ? chGraph.createInEdgeExplorer() : chGraph.createOutEdgeExplorer();
//        outEdgeExplorer = swap ? graph.createEdgeExplorer(AccessFilter.inEdges(flagEncoder.getAccessEnc()))
//                : graph.createEdgeExplorer(AccessFilter.outEdges(flagEncoder.getAccessEnc()));
        this.stoppingCriterion = new MultiSourceStoppingCriterion(targetSet, targetMap, treeEntrySize);

        runAlgo();
        return new AveragedMultiTreeSPEntry[0];
    }

    /**
     * 我们需要将向上阶段找到的所有条目添加到队列中，以便进行可能的向下搜索
     */
    private void addEntriesFromMapToQueue() {
        for (IntObjectCursor<AveragedMultiTreeSPEntry> reachedNode : bestWeightMap)
            prioQueue.add(reachedNode.value);
    }

    protected void runAlgo() {
        RoutingCHEdgeExplorer explorer = swap ? chGraph.createInEdgeExplorer() : chGraph.createOutEdgeExplorer();
        currEdge = prioQueue.poll();
        if (currEdge == null)
            return;

        while (!(isMaxVisitedNodesExceeded())) {
            int currNode = currEdge.getAdjNode();
            boolean isCoreNode = isCoreNode(chGraph, currNode, nodeCount, coreNodeLevel);
            if (isCoreNode) {
                RoutingCHEdgeIterator iter = explorer.setBaseNode(currNode);
                exploreEntry(iter);
            }
            // 如果找到 core 出口节点或子图中的节点，则探索它
            if (coreExitPoints.contains(currNode) || !isCoreNode) {
                RoutingCHEdgeIterator iter = targetGraphExplorer.setBaseNode(currNode);
                exploreEntryDownwards(iter);
            }
            updateTarget(currEdge);
            if (finishedDownwards() || prioQueue.isEmpty())
                break;
            currEdge = prioQueue.poll();
            if (currEdge == null)
                throw new AssertionError("Empty edge cannot happen");
        }
    }

    /**
     * 更新 targetMap 中目标的条目。最终结果将从这里获取。
     * 如果存在 combinedUnsettled 目标，也一并更新。
     *
     * @param update 用于更新目标的条目
     */
    private void updateTarget(AveragedMultiTreeSPEntry update) {
        int nodeId = update.getAdjNode();
        if (targetSet.contains(nodeId)) {
            if (!targetMap.containsKey(nodeId)) {
                AveragedMultiTreeSPEntry newTarget = new AveragedMultiTreeSPEntry(nodeId, EdgeIterator.NO_EDGE, Double.POSITIVE_INFINITY, false, null, update.getSize());
                newTarget.setSubItemOriginalEdgeIds(EdgeIterator.NO_EDGE);
                targetMap.put(nodeId, newTarget);
            }
            AveragedMultiTreeSPEntry target = targetMap.get(nodeId);
            boolean updated = false;
            for (int i = 0; i < treeEntrySize; ++i) {
                MultiTreeSPEntryItem targetItem = target.getItem(i);
                double targetWeight = targetItem.getWeight();

                MultiTreeSPEntryItem msptSubItem = update.getItem(i);
                double updateWeight = msptSubItem.getWeight();

                if (targetWeight > updateWeight) {
                    targetItem.setWeight(updateWeight);
                    targetItem.setEdge(msptSubItem.getEdge());
                    targetItem.setOriginalEdge(msptSubItem.getOriginalEdge());
                    targetItem.setIncEdge(msptSubItem.getIncEdge());
                    targetItem.setParent(msptSubItem.getParent());
                    targetItem.setUpdate(true);
                    updated = true;
                }
            }
            if (updated)
                stoppingCriterion.updateCombinedUnsettled();
        }
    }

    /**
     *
     *
     * _____在核心中搜索
     *
     */

    /**
     * 探索一个条目，适用于有转向限制或无转向限制的情况
     *
     * @param iter
     */
    private void exploreEntry(RoutingCHEdgeIterator iter) {
        while (iter.next()) {
            if (considerTurnRestrictions(iter.getAdjNode())) {
                handleMultiEdgeCase(iter);
            } else {
                handleSingleEdgeCase(iter);
            }
        }
    }

    /**
     * 无转向限制的搜索
     *
     * @param iter
     */
    private void handleSingleEdgeCase(RoutingCHEdgeIterator iter) {
        AveragedMultiTreeSPEntry entry = bestWeightMap.get(iter.getAdjNode());
        if (entry == null) {
            entry = createEmptyEntry(iter);
            boolean addToQueue = iterateMultiTree(iter, entry);
            if (addToQueue) {
                updateEntryInQueue(entry, true);
                bestWeightMap.put(iter.getAdjNode(), entry);
            }
        } else {
            boolean addToQueue = iterateMultiTree(iter, entry);
            if (addToQueue) {
                updateEntryInQueue(entry, false);
            }
        }
    }

    /**
     * 有转向限制的搜索
     *
     * @param iter
     */
    private void handleMultiEdgeCase(RoutingCHEdgeIterator iter) {
        AveragedMultiTreeSPEntry entry = null;
        List<AveragedMultiTreeSPEntry> entries = bestWeightMapCore.get(iter.getAdjNode());

        //选择或生成基于边的条目列表和条目
        if (entries == null)
            entries = createEntriesList(iter);
        else
            entry = getEdgeEntry(iter, entries);
        //处理条目
        if (entry == null) {
            entry = createEmptyEntry(iter);
            boolean addToQueue = iterateMultiTree(iter, entry);
            if (addToQueue) {
                entries.add(entry);
                updateEntryInQueue(entry, true);
            }

        } else {
            boolean addToQueue = iterateMultiTree(iter, entry);
            if (addToQueue) {
                updateEntryInQueue(entry, false);
            }
        }
    }

    /**
     * 遍历一个 MultiTree 条目及其子项以调整新权重
     *
     * @param iter 与 currEdge 相邻的迭代器
     * @return 若任一权重发生更新则返回 true
     */
    private boolean iterateMultiTree(RoutingCHEdgeIterator iter, AveragedMultiTreeSPEntry entry) {
        boolean addToQueue = false;
        visitedNodes++;

        for (int source = 0; source < treeEntrySize; ++source) {
            MultiTreeSPEntryItem currEdgeItem = this.currEdge.getItem(source);
            double entryWeight = currEdgeItem.getWeight();

            if (entryWeight == Double.POSITIVE_INFINITY || !currEdgeItem.isUpdate())
                continue;

            if (stoppingCriterion.isEntryLargerThanAllTargets(source, entryWeight))
                continue;

            MultiTreeSPEntryItem msptSubItem = entry.getItem(source);
            if (!accept(iter, currEdgeItem.getIncEdge(), swap))
                continue;

            double edgeWeight = calcWeight(iter, swap, currEdgeItem.getOriginalEdge());
            if (edgeWeight == Double.POSITIVE_INFINITY)
                continue;

            double tmpWeight = edgeWeight + entryWeight;
            if (stoppingCriterion.isEntryLargerThanAllTargets(source, tmpWeight))
                continue;

            if (msptSubItem.getWeight() > tmpWeight) {
                msptSubItem.setWeight(tmpWeight);
                msptSubItem.setEdge(iter.getEdge());
                msptSubItem.setOriginalEdge(iter.getOrigEdge());
                msptSubItem.setIncEdge(getIncEdge(iter, swap));
                msptSubItem.setParent(this.currEdge);
                msptSubItem.setUpdate(true);
                addToQueue = true;
            }
        }
        return addToQueue;
    }

    /**
     *
     *
     * _____在核心外向下搜索
     *
     */

    /**
     * 使用向下过滤器探索单个条目
     *
     * @param iter 条目上的迭代器
     */
    private void exploreEntryDownwards(RoutingCHEdgeIterator iter) {
        currEdge.resetUpdate(true);
        currEdge.setVisited(true);
        if (iter == null)
            return;

        while (iter.next()) {
            AveragedMultiTreeSPEntry entry = bestWeightMap.get(iter.getAdjNode());

            if (entry == null) {
                entry = createEmptyEntry(iter);
                boolean addToQueue = iterateMultiTreeDownwards(currEdge, iter, entry);
                if (addToQueue) {
                    bestWeightMap.put(iter.getAdjNode(), entry);
                    updateEntryInQueue(entry, true);
                }
            } else {
                boolean addToQueue = iterateMultiTreeDownwards(currEdge, iter, entry);
                if (!entry.isVisited() || addToQueue) {
                    // 如果节点在向上阶段（fillEdges）已被分配权重，
                    // 就会出现这种情况。我们需要在向下阶段使用它来访问更低层级的节点，
                    // 尽管权重不一定需要被重置
                    updateEntryInQueue(entry, false);
                }
            }
        }
    }

    /**
     * 在向下阶段搜索一个条目的所有项
     *
     * @param currEdge 当前边
     * @param iter     遍历当前相邻条目的迭代器
     * @param adjEntry 要在映射中搜索的条目
     * @return
     */
    private boolean iterateMultiTreeDownwards(AveragedMultiTreeSPEntry currEdge, RoutingCHEdgeIterator iter, AveragedMultiTreeSPEntry adjEntry) {
        boolean addToQueue = false;
        visitedNodes++;

        for (int source = 0; source < treeEntrySize; ++source) {
            MultiTreeSPEntryItem currEdgeItem = currEdge.getItem(source);
            double entryWeight = currEdgeItem.getWeight();

            if (entryWeight == Double.POSITIVE_INFINITY)
                continue;
            if (stoppingCriterion.isEntryLargerThanAllTargets(source, entryWeight))
                continue;

            double edgeWeight;
//            configureTurnWeighting(hasTurnWeighting, ((SubGraph.EdgeIteratorLinkIterator) iter).getCurrState(), currEdgeItem);
            edgeWeight = calcWeight(((SubGraph.EdgeIteratorLinkIterator) iter).getCurrState(), swap, currEdgeItem.getOriginalEdge());
//            edgeWeight = weighting.calcEdgeWeight(((SubGraph.EdgeIteratorLinkIterator) iter).getCurrState(), swap, currEdgeItem.getOriginalEdge());
            if (Double.isInfinite(edgeWeight))
                continue;
            double tmpWeight = edgeWeight + entryWeight;

            if (stoppingCriterion.isEntryLargerThanAllTargets(source, tmpWeight))
                continue;

            MultiTreeSPEntryItem eeItem = adjEntry.getItem(source);

            if (eeItem.getWeight() > tmpWeight) {
                eeItem.setWeight(tmpWeight);
                eeItem.setEdge(iter.getEdge());
                eeItem.setOriginalEdge(iter.getOrigEdge());
                eeItem.setIncEdge(getIncEdge(iter, swap));
                eeItem.setParent(currEdge);
                eeItem.setUpdate(true);
                addToQueue = true;
            }
        }
        return addToQueue;
    }

    private AveragedMultiTreeSPEntry createEmptyEntry(RoutingCHEdgeIterator iter) {
        return new AveragedMultiTreeSPEntry(iter.getAdjNode(), iter.getEdge(), Double.POSITIVE_INFINITY, false, null, currEdge.getSize());
    }

    /**
     * 更新优先队列中的现有条目
     *
     * @param entry 要更新的条目
     */
    private void updateEntryInQueue(AveragedMultiTreeSPEntry entry, boolean isNewEntry) {
        if (!isNewEntry)
            prioQueue.remove(entry);
        entry.updateWeights();
        prioQueue.add(entry);
    }

    /**
     * 从条目列表中选择与当前边对应的条目。这基于相邻节点和边 ID。
     *
     * @param iter    要选择的条目
     * @param entries 供选择的列表
     * @return 列表中的条目，若不存在则为 null
     */
    private AveragedMultiTreeSPEntry getEdgeEntry(RoutingCHEdgeIterator iter, List<AveragedMultiTreeSPEntry> entries) {
        AveragedMultiTreeSPEntry entry = null;
        ListIterator<AveragedMultiTreeSPEntry> it = entries.listIterator();
        while (it.hasNext()) {
            AveragedMultiTreeSPEntry listEntry = it.next();
            if (listEntry.getEdge() == iter.getEdge()) {
                entry = listEntry;
                break;
            }
        }
        return entry;
    }

    /**
     * 为给定节点生成条目列表。如果不存在目标节点，则在常规权重映射中初始化该目标节点
     *
     * @param iter 带有待初始化相邻节点的迭代器
     * @return 条目列表
     */
    private List<AveragedMultiTreeSPEntry> createEntriesList(RoutingCHEdgeIterator iter) {
        List<AveragedMultiTreeSPEntry> entries;
        entries = initBestWeightMapEntryList(bestWeightMapCore, iter.getAdjNode());
        //在常规权重映射中初始化目标条目
        if (coreExitPoints.contains(iter.getAdjNode())) {
            AveragedMultiTreeSPEntry target = bestWeightMap.get(iter.getAdjNode());
            if (target == null) {
                target = createEmptyEntry(iter);
                bestWeightMap.put(iter.getAdjNode(), target);
            }
        }
        return entries;
    }

    List<AveragedMultiTreeSPEntry> initBestWeightMapEntryList(IntObjectMap<List<AveragedMultiTreeSPEntry>> map, int traversalId) {
        if (map.get(traversalId) != null)
            throw new IllegalStateException("Core entry point already exists in best weight map.");

        List<AveragedMultiTreeSPEntry> entryList = new ArrayList<>(5);
        map.put(traversalId, entryList);

        return entryList;
    }

    /**
     * @return 是否已找到所有目标节点
     */
    private boolean finishedDownwards() {
        //检查是否已为所有源找到所有目标
        return stoppingCriterion.isFinished(currEdge, prioQueue);
    }

    public void setTargetGraphExplorer(RoutingCHEdgeExplorer targetGraphExplorer) {
        this.targetGraphExplorer = targetGraphExplorer;
    }

    public void setTargetMap(IntObjectMap<AveragedMultiTreeSPEntry> targetMap) {
        this.targetMap = targetMap;
    }

    public void setTargetSet(IntHashSet targetSet) {
        this.targetSet = targetSet;
    }

    boolean considerTurnRestrictions(int node) {
        return hasTurnWeighting;
    }

    public void setHasTurnWeighting(boolean hasTurnWeighting) {
        this.hasTurnWeighting = hasTurnWeighting;
    }

    public void setTreeEntrySize(int entrySize) {
        this.treeEntrySize = entrySize;
    }

    public void setSwap(boolean swap) {
        this.swap = swap;
    }


    @Override
    public int getVisitedNodes() {
        return visitedNodes;
    }

    public void setVisitedNodes(int numberOfNodes) {
        this.visitedNodes = numberOfNodes;
    }

    @Override
    public void setMaxVisitedNodes(int numberOfNodes) {
        this.maxVisitedNodes = numberOfNodes;
    }

    @Override
    public String getName() {
        return Parameters.Algorithms.DIJKSTRA;
    }

    //TODO 重构：将权重相关逻辑移至辅助类
    double calcPathWeight(RoutingCHEdgeIteratorState iter, SPTEntry currEdge, boolean reverse) {
        return calcWeight(iter, reverse, currEdge.originalEdge) + currEdge.getWeightOfVisitedPath();
    }

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
