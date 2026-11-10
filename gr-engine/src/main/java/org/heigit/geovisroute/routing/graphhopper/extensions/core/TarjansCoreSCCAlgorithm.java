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

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntArrayList;
import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.core.CoreLandmarkStorage.CoreEdgeFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GraphUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 使用显式栈实现的 Tarjan 算法。传统的递归方式
 * 很快就会栈溢出。该算法在 GraphHopper 中用于查找
 * 强连通分量，以检测导致路线无法找到的死胡同。
 * <p>
 * 参见 http://en.wikipedia.org/wiki/Tarjan's_strongly_connected_components_algorithm。参见
 * http://www.timl.id.au/?p=327 和 http://homepages.ecs.vuw.ac.nz/~djp/files/P05.pdf
 * <p>
 * <p>
 * 此实现已针对核心区中的使用进行了调整。
 * <p>
 * 本代码基于 GraphHopper GmbH 的代码。
 *
 * @author Peter Karich
 * @author Hendrik Leuschner
 */
public class TarjansCoreSCCAlgorithm {
    private final ArrayList<IntArrayList> components = new ArrayList<>();
    // TODO 重构：检查此注释是否抄自 GH 代码，可能已无关："use just the Graph interface here"
    private final IntArrayDeque nodeStack;
    private final GHBitSet onStack;
    private final GHBitSet ignoreSet;
    private final int[] nodeIndex;
    private final int[] nodeLowLink;
    private final CHEdgeFilter edgeFilter;
    private int index = 1;
    private final RoutingCHGraph core;
    private final int coreNodeLevel;

    public TarjansCoreSCCAlgorithm(GRGraph baseGraph, RoutingCHGraph core, final EdgeFilter edgeFilter, boolean ignoreSingleEntries) {
        this.core = core;
        this.nodeStack = new IntArrayDeque();
        this.onStack = new GHBitSetImpl(baseGraph.getNodes());
        this.nodeIndex = new int[baseGraph.getNodes()];
        this.nodeLowLink = new int[baseGraph.getNodes()];
        CoreEdgeFilter coreEdgeFilter = new CoreEdgeFilter(core, edgeFilter);
        this.edgeFilter = edge -> coreEdgeFilter.accept(edge) && Double.isFinite(edge.getWeight(false));
        this.coreNodeLevel = GraphUtils.getBaseGraph(core).getNodes();


        if (ignoreSingleEntries) {
            // 对提升性能非常重要的情形 - 参见 #520。排除单入口分量，因为不需要它们！
            // 但它们会因多种车辆而大量产生，因为例如许多对行人可通行的节点对汽车完全不可达。
            // 我们可以忽略这些单入口分量，因为它们已被标记为 '不可达'
            RoutingCHEdgeExplorer explorer = core.createOutEdgeExplorer();
            int nodes = baseGraph.getNodes();
            ignoreSet = new GHBitSetImpl(baseGraph.getCoreNodes());
            for (int start = 0; start < nodes; start++) {
                CoreEdgeIterator iter = new CoreEdgeIterator(explorer.setBaseNode(start), this.edgeFilter);
                if (!iter.next())
                    ignoreSet.add(start);
            }
        } else {
            ignoreSet = new GHBitSetImpl();
        }
    }

    public GHBitSet getIgnoreSet() {
        return ignoreSet;
    }

    /**
     * 查找并返回 g 中所有强连通分量的列表。
     */
    public List<IntArrayList> findComponents() {
        int nodes = core.getNodes();
        for (int start = 0; start < nodes; start++) {
            if (core.getLevel(start) < coreNodeLevel)
                continue;
            if (nodeIndex[start] == 0 && !ignoreSet.contains(start))
                strongConnect(start);
        }

        return components;
    }

    /**
     * 查找从 firstNode 可达的所有分量，并将其加入 'components'
     * <p>
     *
     * @param firstNode 在此节点开始 SCC 搜索
     */
    private void strongConnect(int firstNode) {
        final Deque<TarjanState> stateStack = new ArrayDeque<>();
        stateStack.push(TarjanState.startState(firstNode));

        // nextState 标签等效于递归 Tarjan 算法中的函数入口点。
        nextState:

        while (!stateStack.isEmpty()) {
            TarjanState state = stateStack.pop();
            final int start = state.start;
            final RoutingCHEdgeIterator iter;

            if (state.isStart()) {
                // 我们正在遍历一个新节点 'start'。将该节点的深度索引设为其最小未使用索引。
                nodeIndex[start] = index;
                nodeLowLink[start] = index;
                index++;
                nodeStack.addLast(start);
                onStack.add(start);

                iter = new CoreEdgeIterator(core.createOutEdgeExplorer().setBaseNode(start), edgeFilter);

            } else {
                // 我们正在恢复对 'start' 下一个子节点的迭代，按需设置 lowLink。
                iter = state.iter;

                int prevConnectedId = iter.getAdjNode();
                nodeLowLink[start] = Math.min(nodeLowLink[start], nodeLowLink[prevConnectedId]);
            }

            // 当前分量中的每个元素（第一个除外）都应能够找到
            // 一个具有更小 nodeLowLink 的后继。
            while (iter.next()) {
                int connectedId = iter.getAdjNode();
                if (ignoreSet.contains(start) || core.getLevel(start) < coreNodeLevel)
                    continue;

                if (nodeIndex[connectedId] == 0) {
                    // 将恢复状态和起始状态压入状态栈，以便在跳转后继续对图进行 DFS。
                    // 理想情况下我们只需调用 strongConnectIterative(connectedId);
                    stateStack.push(TarjanState.resumeState(start, iter));
                    stateStack.push(TarjanState.startState(connectedId));
                    continue nextState;
                } else if (onStack.contains(connectedId)) {
                    nodeLowLink[start] = Math.min(nodeLowLink[start], nodeIndex[connectedId]);
                }
            }

            // 如果 nodeLowLink == nodeIndex，则我们是该分量中的第一个元素。
            // 将 nodeStack 上更高位置的所有节点加入此分量。
            if (nodeIndex[start] == nodeLowLink[start]) {
                IntArrayList component = new IntArrayList();
                int node;
                while ((node = nodeStack.removeLast()) != start) {
                    component.add(node);
                    onStack.remove(node);
                }
                component.add(start);
                component.trimToSize();
                onStack.remove(start);
                components.add(component);
            }
        }
    }

    /**
     * 算法的内部栈状态，用于避免递归函数调用和栈
     * 溢出异常。状态对于新节点为 'start'，对于部分
     * 遍历的节点为 'resume'。
     */
    private static class TarjanState {
        final int start;
        final RoutingCHEdgeIterator iter;

        private TarjanState(final int start, final RoutingCHEdgeIterator iter) {
            this.start = start;
            this.iter = iter;
        }

        public static TarjanState startState(int start) {
            return new TarjanState(start, null);
        }

        public static TarjanState resumeState(int start, RoutingCHEdgeIterator iter) {
            return new TarjanState(start, iter);
        }

        // 迭代器仅存在于 'resume' 状态中。
        boolean isStart() {
            return iter == null;
        }
    }

    private static class CoreEdgeIterator implements RoutingCHEdgeIterator {
        private final RoutingCHEdgeIterator chIterator;
        private final CHEdgeFilter coreFilter;

        public CoreEdgeIterator(RoutingCHEdgeIterator chIterator, CHEdgeFilter coreFilter) {
            this.chIterator = chIterator;
            this.coreFilter = coreFilter;
        }

        @Override
        public boolean next() {
            while (chIterator.next()) {
                if (coreFilter.accept(chIterator))
                    return true;
            }
            return false;
        }

        @Override
        public int getEdge() {
            return chIterator.getEdge();
        }

        @Override
        public int getOrigEdge() {
            return chIterator.getOrigEdge();
        }

        @Override
        public int getOrigEdgeKeyFirst() {
            return chIterator.getOrigEdgeKeyFirst();
        }

        @Override
        public int getOrigEdgeKeyLast() {
            return chIterator.getOrigEdgeKeyLast();
        }

        @Override
        public int getBaseNode() {
            return chIterator.getBaseNode();
        }

        @Override
        public int getAdjNode() {
            return chIterator.getAdjNode();
        }

        @Override
        public boolean isShortcut() {
            return chIterator.isShortcut();
        }

        @Override
        public int getSkippedEdge1() {
            return chIterator.getSkippedEdge1();
        }

        @Override
        public int getSkippedEdge2() {
            return chIterator.getSkippedEdge2();
        }

        @Override
        public double getWeight(boolean reverse) {
            return chIterator.getWeight(reverse);
        }

        @Override
        public int getTime(boolean reverse) {
            return chIterator.getTime(reverse);
        }
    }
}
