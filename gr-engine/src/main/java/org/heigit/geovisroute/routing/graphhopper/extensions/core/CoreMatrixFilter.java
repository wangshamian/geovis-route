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

import com.graphhopper.storage.RoutingCHEdgeIteratorState;
import com.graphhopper.storage.RoutingCHGraph;

/**
 * 只接受特定节点，其余节点被忽略。
 * <p>
 * 本代码基于 GraphHopper GmbH 的代码。
 *
 * @author Peter Karich
 * @author Andrzej Oles, Hendrik Leuschner
 */
public class CoreMatrixFilter extends CoreDijkstraFilter {

    /**
     * @param graph
     */
    public CoreMatrixFilter(RoutingCHGraph graph) {
        super(graph);
    }

    /**
     * @param edgeIterState 指向给定边的迭代器
     * @return 当边是虚拟边、或是快捷边、或基础节点层级大于/等于相邻节点层级时为 true
     */
    @Override
    public boolean accept(RoutingCHEdgeIteratorState edgeIterState) {
        int base = edgeIterState.getBaseNode();
        int adj = edgeIterState.getAdjNode();

        if (!inCore) {
            // 始终接受虚拟边，参见 #288
            if (base >= maxNodes || adj >= maxNodes)
                return true;
            // 小幅性能优化：方向错误的快捷边已被断开，因此无需检查
            if (edgeIterState.isShortcut())
                return true;
            else
                return graph.getLevel(base) <= graph.getLevel(adj);
        } else {
            //这是与父类 CoreDijkstraFilter 的区别所在：接受虚拟边，因为在矩阵请求中图内有超过 2 个虚拟节点。
            if (base >= maxNodes || adj >= maxNodes)
                return true;
            // 小幅性能优化：方向错误的快捷边已被断开，因此无需检查
            if (edgeIterState.isShortcut())
                return true;

            // 不跟随虚拟边，并保持在核心区内
            if (isCoreNode(adj))
                // 如果边位于核心区内，则检查限制条件
                return restrictions == null || restrictions.accept(graph.getBaseGraph().getEdgeIteratorState(edgeIterState.getEdge(), adj));
            else
                return false;
        }
    }
}
