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
package org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.routing.RouteSearchParameters;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.BordersGraphStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.pathprocessors.BordersExtractor;

public class AvoidBordersEdgeFilter implements EdgeFilter {
    private BordersExtractor.Avoid avoidBorders = BordersExtractor.Avoid.NONE;
    private boolean avoidCountries = false;
    private boolean isStorageBuilt;

    private BordersExtractor bordersExtractor;

    public AvoidBordersEdgeFilter(RouteSearchParameters searchParams, BordersGraphStorage extBorders) {
        init(searchParams, extBorders);
    }

    public AvoidBordersEdgeFilter(RouteSearchParameters searchParams, BaseGraph basegraph) {
        BordersGraphStorage extBorders = GraphStorageUtils.getGraphExtension(basegraph, BordersGraphStorage.class);
        init(searchParams, extBorders);
    }

    /**
     * 根据要过滤的国界类型初始化边过滤器对象
     *
     * @param searchParams 传入请求的搜索参数
     * @param extBorders   要使用的扩展国界图存储
     */
    private void init(RouteSearchParameters searchParams, BordersGraphStorage extBorders) {
        // 初始化图存储
        isStorageBuilt = extBorders != null;
        if (isStorageBuilt) {
            int[] countriesToAvoid;
            if (searchParams.hasAvoidCountries())
                countriesToAvoid = searchParams.getAvoidCountries();
            else
                countriesToAvoid = new int[0];

            this.avoidCountries = countriesToAvoid.length > 0;

            if (searchParams.hasAvoidBorders()) {
                avoidBorders = searchParams.getAvoidBorders();
            }

            bordersExtractor = new BordersExtractor(extBorders, countriesToAvoid);
        }
    }

    /**
     * 判断该边应被接受处理还是拒绝。请求是“不跨越任何国界”还是“不跨越受管控的国界”，
     * 决定了要拒绝的国界类型。
     *
     * @param iter 指向需要过滤的边的迭代器
     * @return
     */
    @Override
    public final boolean accept(EdgeIteratorState iter) {
        if (!isStorageBuilt)
            return true;

        if (avoidBorders != BordersExtractor.Avoid.NONE) {
            // 已要求避让某种形式的国界
            switch (avoidBorders) {
                case ALL:
                    if (bordersExtractor.isBorder(iter.getEdge())) {
                        // 这是一条国界，而我们希望避让所有国界
                        return false;
                    }
                    break;
                case CONTROLLED:
                    if (bordersExtractor.isControlledBorder(iter.getEdge())) {
                        // 只希望避让受管控的国界
                        return false;
                    }
                    break;
                default:
                    break;
            }
        }

        return !avoidCountries || !bordersExtractor.restrictedCountry(iter.getEdge());
    }

}
