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
package org.heigit.geovisroute.routing.algorithms;

import com.graphhopper.routing.SPTEntry;

public interface OneToManyRoutingAlgorithm {

    void prepare(int[] from, int[] to);

    SPTEntry[] calcPaths(int from, int[] to);

    void reset();

    /**
     * 将搜索限制为 numberOfNodes 个节点。见 #681
     */
    void setMaxVisitedNodes(int numberOfNodes);

    /**
     * @return 该算法的名称
     */
    String getName();

    /**
     * 返回搜索后访问的节点数。用于调试。
     */
    int getVisitedNodes();
}
