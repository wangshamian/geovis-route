package org.heigit.geovisroute.routing.graphhopper.extensions.util;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.AveragedMultiTreeSPEntry;

import java.util.PriorityQueue;

public class MultiSourceStoppingCriterion {
    private final int treeEntrySize;
    private AveragedMultiTreeSPEntry combinedUnsettled;
    private final IntHashSet targetSet;
    IntObjectMap<AveragedMultiTreeSPEntry> targetMap;
    IntObjectMap<Boolean> allTargetsForSourceFound;


    public MultiSourceStoppingCriterion(IntHashSet targetSet, IntObjectMap<AveragedMultiTreeSPEntry> targetMap, int treeEntrySize) {
        this.targetSet = targetSet;
        this.targetMap = targetMap;
        this.treeEntrySize = treeEntrySize;
        this.allTargetsForSourceFound = new IntObjectHashMap<>(treeEntrySize);
    }

    public boolean isFinished(AveragedMultiTreeSPEntry currEdge, PriorityQueue<AveragedMultiTreeSPEntry> prioQueue) {
        if (combinedUnsettled != null && checkAllTargetsForAllSourcesFound())
            return !queueHasSmallerWeight(combinedUnsettled, prioQueue);

        if (!targetSet.contains(currEdge.getAdjNode()))
            return false;

        setSourceTargetsFound();
        createCombinedUnsettled();

        return false;
    }

    /**
     * 创建一个单一的非真实目标条目，合并所有未确定节点的各自最大权重
     * 这样做是为了尽量减少对未确定目标的遍历，而为保证正确性，必须遍历
     * 直到优先队列中不再有更优的可能值
     */
    private void createCombinedUnsettled() {
        if (this.combinedUnsettled == null)
            this.combinedUnsettled = initCombinedUnsettled();
        updateCombinedUnsettled();
    }

    private AveragedMultiTreeSPEntry initCombinedUnsettled() {
        AveragedMultiTreeSPEntry combinedUnsettledTarget = new AveragedMultiTreeSPEntry(-1, -1, -1.0, false, null, treeEntrySize);
        //将所有权重设置为较低的初始权重
        for (int i = 0; i < treeEntrySize; ++i)
            combinedUnsettledTarget.getItem(i).setWeight(-1.0);

        return combinedUnsettledTarget;
    }

    public void updateCombinedUnsettled() {
        if (combinedUnsettled == null)
            return;
        for (IntObjectCursor<AveragedMultiTreeSPEntry> entry : targetMap) {
            for (int source = 0; source < treeEntrySize; ++source) {
                if (allTargetsForSourceFound.getOrDefault(source, false)) {

                    double entryWeight = entry.value.getItem(source).getWeight();

                    if (entryWeight > this.combinedUnsettled.getItem(source).getWeight()) {
                        this.combinedUnsettled.getItem(source).setWeight(entryWeight);
                    }
                }
            }
        }
    }

    /**
     * 检查优先队列中是否存在可能为任一子项带来更短路径的条目
     *
     * @return
     */
    private boolean queueHasSmallerWeight(AveragedMultiTreeSPEntry target, PriorityQueue<AveragedMultiTreeSPEntry> prioQueue) {
        for (AveragedMultiTreeSPEntry entry : prioQueue) {
            for (int i = 0; i < treeEntrySize; ++i) {
                if (entry.getItem(i).getWeight() < target.getItem(i).getWeight())
                    return true;
            }
        }
        return false;
    }

    private boolean checkAllTargetsForAllSourcesFound() {
        for (int source = 0; source < treeEntrySize; source++) {
            if (combinedUnsettled.getItem(source).getWeight() == -1.0)
                return false;
        }
        return true;
    }

    private void setSourceTargetsFound() {
        for (int source = 0; source < treeEntrySize; source += 1) {
            if (allTargetsForSourceFound.getOrDefault(source, false))
                continue;
            boolean allFound = true;
            for (IntCursor targetId : targetSet) {
                //该目标尚未被到达
                if (!targetMap.containsKey(targetId.value))
                    return;
                AveragedMultiTreeSPEntry target = targetMap.get(targetId.value);
                if (target.getItem(source).getWeight() == Double.POSITIVE_INFINITY) {
                    allFound = false;
                    break;
                }

            }
            allTargetsForSourceFound.put(source, allFound);
        }
    }

    public boolean isEntryLargerThanAllTargets(int source, double weight) {
        return combinedUnsettled != null
                && combinedUnsettled.getItem(source).getWeight() != -1.0
                && weight > combinedUnsettled.getItem(source).getWeight();
    }
}
