package org.heigit.geovisroute.routing.graphhopper.extensions.core;

import com.carrotsearch.hppc.IntContainer;
import com.graphhopper.routing.ch.*;
import com.graphhopper.storage.CHStorageBuilder;
import com.graphhopper.util.PMap;
import com.graphhopper.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.graphhopper.routing.ch.CHParameters.EDGE_DIFFERENCE_WEIGHT;
import static com.graphhopper.routing.ch.CHParameters.ORIGINAL_EDGE_COUNT_WEIGHT;
import static com.graphhopper.util.Helper.nf;

class CoreNodeContractor implements NodeContractor {
    private final CorePreparationGraph prepareGraph;
    private final Params params = new Params();
    // todo: 也许改用集合来防止重复？
    private List<Shortcut> shortcuts = new ArrayList<>();
    private CHStorageBuilder chBuilder;
    private PrepareGraphEdgeExplorer inEdgeExplorer;
    private PrepareGraphEdgeExplorer outEdgeExplorer;
    private PrepareGraphEdgeExplorer existingShortcutExplorer;
    private NodeBasedWitnessPathSearcher witnessPathSearcher;
    private int addedShortcutsCount;
    private long dijkstraCount;
    private final StopWatch dijkstraSW = new StopWatch();
    // meanDegree 是图的边数 / 节点数之比，并非真正的平均度数，因为每条边可以存在于两个方向
    private double meanDegree;
    // 用于计算优先级的临时计数器
    private int originalEdgesCount;
    private int shortcutsCount;

    CoreNodeContractor(CorePreparationGraph prepareGraph, CHStorageBuilder chBuilder, PMap pMap) {
        this.prepareGraph = prepareGraph;
        extractParams(pMap);
        this.chBuilder = chBuilder;
    }

    private void extractParams(PMap pMap) {
        params.edgeDifferenceWeight = pMap.getFloat(EDGE_DIFFERENCE_WEIGHT, params.edgeDifferenceWeight);
        params.originalEdgesCountWeight = pMap.getFloat(ORIGINAL_EDGE_COUNT_WEIGHT, params.originalEdgesCountWeight);
    }

    @Override
    public void initFromGraph() {
        inEdgeExplorer = prepareGraph.createInEdgeExplorer();
        outEdgeExplorer = prepareGraph.createOutEdgeExplorer();
        existingShortcutExplorer = prepareGraph.createOutEdgeExplorer();
        witnessPathSearcher = new NodeBasedWitnessPathSearcher(prepareGraph);
    }

    public void prepareContraction() {
        // todo: 在这里而非 initFromGraph() 中初始化 meanDegree 意味着在计算节点优先级的第一轮中，
        // 所有快捷边搜索都会立即被取消，并且会统计所有可能的快捷边，因为找不到任何见证路径。
        // 这并非我们真正想要的，但修改它需要重新优化图收缩参数，因为这会影响节点收缩顺序。
        // 完成这些之后，应不再需要此方法。
        meanDegree = prepareGraph.getOriginalEdges() / (double) prepareGraph.getNodes();
    }

    @Override
    public void close() {
        prepareGraph.close();
        shortcuts = null;
        chBuilder = null;
        inEdgeExplorer = null;
        outEdgeExplorer = null;
        existingShortcutExplorer = null;
        witnessPathSearcher = null;
    }

    /**
     * 警告：计算出的优先级绝不能依赖于 priority(v)，因此 findAndHandleShortcuts 也不应
     * 依赖于 priority(v)。否则在 contractNodes() 中收缩前更新优先级可能导致
     * 变慢甚至死循环。
     */
    @Override
    public float calculatePriority(int node) {
        // # 影响巨大：值越大创建的快捷边越少，预处理越快
        //
        // 每个 adjNode 都关联一个 'original edge' 数。初始时 r=1
        // 当引入一条新快捷边时，相关边的 r 会相加：
        // r(u,w)=r(u,v)+r(v,w) 现在我们可以定义
        // originalEdgesCount = σ(v) := sum_{ (u,w) ∈ shortcuts(v) } of r(u, w)
        shortcutsCount = 0;
        originalEdgesCount = 0;
        findAndHandleShortcuts(node, this::countShortcuts);

        // 从快捷边可以计算 edgeDifference
        // # 影响较小：借助它快捷边的创建会稍快一些
        //
        // |shortcuts(v)| − |{(u, v) | v uncontracted}| − |{(v, w) | v uncontracted}|
        // 使用 meanDegree 而不是 outDegree+inDegree，因为如果某个 adjNode 在两个方向上
        // 只使用一份桶内存。此外一条快捷边也可能代表两个方向。
        int edgeDifference = shortcutsCount - prepareGraph.getDegree(node);

        // 根据论文，对这些属性做简单的线性组合以得到优先级。
        return params.edgeDifferenceWeight * edgeDifference +
                params.originalEdgesCountWeight * originalEdgesCount;
        // todo: 是否像 GH 1.0 那样再次使用已收缩邻居启发式（对拥有大量已收缩邻居的节点稍后收缩）？
        //       也许像基于边的方式那样使用层级深度启发式？
    }

    @Override
    public IntContainer contractNode(int node) {
        long degree = findAndHandleShortcuts(node, this::addOrUpdateShortcut);
        insertShortcuts(node, true);
        // 将权重因子施加在 meanDegree 上而非取平均值 => meanDegree 更稳定
        meanDegree = (meanDegree * 2 + degree) / 3;
        return prepareGraph.disconnect(node);
    }

    /**
     * 对与给定节点相邻的所有边和快捷边调用快捷边处理器。此方法调用后
     * 这些边和快捷边将从预备图中移除，因此此方法提供了处理它们的最后机会。
     */
    protected void insertShortcuts(int node, boolean outsideCore) {
        shortcuts.clear();
        insertOutShortcuts(node);
        insertInShortcuts(node);
        int origEdges = prepareGraph.getOriginalEdges();
        for (Shortcut sc : shortcuts) {
            int shortcut = chBuilder.addShortcutCore(sc.from, sc.to, sc.flags, sc.weight, sc.skippedEdge1, sc.skippedEdge2, sc.time);
            if (outsideCore) {
                if (sc.flags == PrepareEncoder.getScFwdDir()) {
                    prepareGraph.setShortcutForPrepareEdge(sc.prepareEdgeFwd, origEdges + shortcut);
                } else if (sc.flags == PrepareEncoder.getScBwdDir()) {
                    prepareGraph.setShortcutForPrepareEdge(sc.prepareEdgeBwd, origEdges + shortcut);
                } else {
                    prepareGraph.setShortcutForPrepareEdge(sc.prepareEdgeFwd, origEdges + shortcut);
                    prepareGraph.setShortcutForPrepareEdge(sc.prepareEdgeBwd, origEdges + shortcut);
                }
            }
        }
        addedShortcutsCount += shortcuts.size();
    }

    private void insertOutShortcuts(int node) {
        PrepareGraphEdgeIterator iter = outEdgeExplorer.setBaseNode(node);
        while (iter.next()) {
            if (!iter.isShortcut())
                continue;

            shortcuts.add(new Shortcut(iter.getPrepareEdge(), -1, node, iter.getAdjNode(), iter.getSkipped1(),
                    iter.getSkipped2(), PrepareEncoder.getScFwdDir(), iter.getWeight(), iter.getTime()));
        }
    }

    private void insertInShortcuts(int node) {
        PrepareGraphEdgeIterator iter = inEdgeExplorer.setBaseNode(node);
        while (iter.next()) {
            if (!iter.isShortcut())
                continue;

            int skippedEdge1 = iter.getSkipped2();
            int skippedEdge2 = iter.getSkipped1();
            // 我们检查另一方向是否已存在此快捷边（权重相同），若存在则可将其
            // 同时用于两个方向，而不必再添加一条
            boolean bidir = false;
            for (Shortcut sc : shortcuts) {
                if (sc.to == iter.getAdjNode()
                        && Double.doubleToLongBits(sc.weight) == Double.doubleToLongBits(iter.getWeight())
                        // todo: 难道不能只比较 skippedEdges 吗？
                        && prepareGraph.getShortcutForPrepareEdge(sc.skippedEdge1) == prepareGraph.getShortcutForPrepareEdge(skippedEdge1)
                        && prepareGraph.getShortcutForPrepareEdge(sc.skippedEdge2) == prepareGraph.getShortcutForPrepareEdge(skippedEdge2)
                        && sc.flags == PrepareEncoder.getScFwdDir()) {
                    sc.flags = PrepareEncoder.getScDirMask();
                    sc.prepareEdgeBwd = iter.getPrepareEdge();
                    bidir = true;
                    break;
                }
            }
            if (!bidir) {
                shortcuts.add(new Shortcut(-1, iter.getPrepareEdge(), node, iter.getAdjNode(), skippedEdge1, skippedEdge2, PrepareEncoder.getScBwdDir(), iter.getWeight(), iter.getTime()));
            }
        }
    }

    @Override
    public void finishContraction() {
        // 收缩期间快捷边的 skip1/2 边引用的是预备边 id 而 *不是* 最终的快捷边
        // id（因为在插入之前尚不可知）-> 我们需要在此重新映射这些 id
        chBuilder.replaceSkippedEdges(prepareGraph::getShortcutForPrepareEdge);
    }

    @Override
    public String getStatisticsString() {
        return String.format(Locale.ROOT, "meanDegree: %.2f, dijkstras: %10s, mem: %10s",
                meanDegree, nf(dijkstraCount), witnessPathSearcher.getMemoryUsageAsString());
    }

    /**
     * 搜索快捷边并对找到的每条快捷边调用给定的处理器。此方法不直接
     * 修改图。
     * 返回给定节点的 '度'（不考虑来自/通向已收缩节点的边）。
     * 注意这里的度不是相邻边的总数，而仅是入边的数量
     */
    private long findAndHandleShortcuts(int node, PrepareShortcutHandler handler) {
        int maxVisitedNodes = getMaxVisitedNodesEstimate();
        long degree = 0;
        PrepareGraphEdgeIterator incomingEdges = inEdgeExplorer.setBaseNode(node);
        // 仅收集一次出向节点（目标节点）
        while (incomingEdges.next()) {
            int fromNode = incomingEdges.getAdjNode();
            // 不考虑正在收缩的节点上的自环
            if (fromNode == node)
                continue;

            final double incomingEdgeWeight = incomingEdges.getWeight();
            // 此检查很重要，可防止对不可达边调用 calcMillis，同时也允许提前退出
            if (Double.isInfinite(incomingEdgeWeight)) {
                continue;
            }
            // 仅收集一次出向节点（目标节点）
            PrepareGraphEdgeIterator outgoingEdges = outEdgeExplorer.setBaseNode(node);
            witnessPathSearcher.init(fromNode, node);
            degree++;
            while (outgoingEdges.next()) {
                int toNode = outgoingEdges.getAdjNode();
                // 不考虑正在收缩的节点上的自环
                if (toNode == node || fromNode == toNode)
                    continue;

                // 限制权重，因为渡轮或禁行边可能使局部搜索代价过大。
                // 如果降低正确的权重，我们只会探索更少并引入更多快捷边。
                // 也就是说，不改变精度。
                double existingDirectWeight = incomingEdgeWeight + outgoingEdges.getWeight();
                if (Double.isInfinite(existingDirectWeight))
                    continue;

                dijkstraSW.start();
                dijkstraCount++;
                double maxWeight = witnessPathSearcher.findUpperBound(toNode, existingDirectWeight, maxVisitedNodes);
                dijkstraSW.stop();

                if (maxWeight <= existingDirectWeight)
                    // 找到了见证路径，因此不添加快捷边
                    continue;

                int time = incomingEdges.getTime() + outgoingEdges.getTime();

                handler.handleShortcut(fromNode, toNode, existingDirectWeight, time,
                        outgoingEdges.getPrepareEdge(), outgoingEdges.getOrigEdgeCount(),
                        incomingEdges.getPrepareEdge(), incomingEdges.getOrigEdgeCount());
            }
        }
        return degree;
    }

    private void countShortcuts(int fromNode, int toNode, double existingDirectWeight, int time,
                                int outgoingEdge, int outOrigEdgeCount,
                                int incomingEdge, int inOrigEdgeCount) {
        shortcutsCount++;
        originalEdgesCount += inOrigEdgeCount + outOrigEdgeCount;
    }

    private void addOrUpdateShortcut(int fromNode, int toNode, double weight, int time,
                                     int outgoingEdge, int outOrigEdgeCount,
                                     int incomingEdge, int inOrigEdgeCount) {
        boolean exists = false;
        PrepareGraphEdgeIterator iter = existingShortcutExplorer.setBaseNode(fromNode);
        while (iter.next()) {
            // 不要更新基础边！
            if (iter.getAdjNode() != toNode || !iter.isShortcut()) {
                continue;
            }
            exists = true;
            if (weight < iter.getWeight()) {
                iter.setWeight(weight);
                iter.setSkippedEdges(incomingEdge, outgoingEdge);
                iter.setOrigEdgeCount(inOrigEdgeCount + outOrigEdgeCount);
            }
        }
        if (!exists)
            prepareGraph.addShortcut(fromNode, toNode, incomingEdge, outgoingEdge, weight, time, inOrigEdgeCount + outOrigEdgeCount);
    }

    @Override
    public long getAddedShortcutsCount() {
        return addedShortcutsCount;
    }

    public long getDijkstraCount() {
        return dijkstraCount;
    }

    @Override
    public float getDijkstraSeconds() {
        return dijkstraSW.getCurrentSeconds();
    }

    private int getMaxVisitedNodesEstimate() {
        // todo: 当 meanDegree < 1 时这里会返回 0，这并非我们真正想要的，但修改它会改变
        // 节点收缩顺序，并需要重新优化图收缩的参数
        return (int) meanDegree * 100;
    }

    @FunctionalInterface
    private interface PrepareShortcutHandler {
        void handleShortcut(int fromNode, int toNode, double existingDirectWeight, int time,
                            int outgoingEdge, int outOrigEdgeCount,
                            int incomingEdge, int inOrigEdgeCount);
    }

    public static class Params {
        // 默认值针对 Unterfranken 进行了优化
        private float edgeDifferenceWeight = 10;
        private float originalEdgesCountWeight = 1;
    }

    private static class Shortcut {
        int prepareEdgeFwd;
        int prepareEdgeBwd;
        int from;
        int to;
        int skippedEdge1;
        int skippedEdge2;
        double weight;
        int time;
        int flags;

        public Shortcut(int prepareEdgeFwd, int prepareEdgeBwd, int from, int to, int skippedEdge1, int skippedEdge2, int flags, double weight, int time) {
            this.prepareEdgeFwd = prepareEdgeFwd;
            this.prepareEdgeBwd = prepareEdgeBwd;
            this.from = from;
            this.to = to;
            this.skippedEdge1 = skippedEdge1;
            this.skippedEdge2 = skippedEdge2;
            this.flags = flags;
            this.weight = weight;
            this.time = time;
        }

        @Override
        public String toString() {
            String str;
            if (flags == PrepareEncoder.getScDirMask())
                str = from + "<->";
            else
                str = from + "->";

            return str + to + ", weight:" + weight + " (" + skippedEdge1 + "," + skippedEdge2 + ")";
        }
    }

}

