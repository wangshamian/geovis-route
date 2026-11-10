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
package org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.routing.RouteSearchParameters;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LMEdgeFilterSequence extends EdgeFilterSequence implements EdgeFilter {

    /**
     * 检查某个 LMSet 是否与查询的 PMap 相匹配
     * 如果 LMSet 的属性与请求相同，或是请求属性的子集，则该 LMSet 可以使用。
     * 此时 landmark 启发式是可采纳的，即它只可能低估实际距离。
     * 如果 LMSet 的限制多于查询，则可能高估，启发式就是错误的
     *
     * @return 若该 lmset 可用于计算该查询则返回 true
     */
    public boolean isFilter(PMap pmap) {
        //若 avoidFeatures 部分与查询相匹配则为 true
        RouteSearchParameters routeSearchParameters = pmap.getObject("avoid_features", new RouteSearchParameters());
        boolean avoidFeatures = isAvoidFeature(routeSearchParameters.getAvoidFeatureTypes());
        boolean avoidCountries = isAvoidCountry(pmap.getString("avoid_countries", ""));
        return avoidFeatures && avoidCountries;


    }

    /**
     * 检查 LMSet 中指定的避让国家是否为查询中所请求避让国家的子集
     *
     * @return 若该 lmset 可用于计算该查询则返回 true
     */
    private boolean isAvoidCountry(String countries) {
        ArrayList<Integer> queryCountries = new ArrayList<>();
        if (!(countries.equals("") || countries.equals("[]"))) {
            //去掉方括号
            countries = countries.substring(1, countries.length() - 1);
            String[] countryList = countries.split(", ");
            //构建一个 int 型 arraylist
            for (int i = 0; i < countryList.length; i++) {
                queryCountries.add(Integer.parseInt(countryList[i]));
            }
        }
        //检查 avoidBordersFilter 是否具有相同的国家或为其子集
        for (EdgeFilter edgeFilter : filters) {
            if (edgeFilter instanceof AvoidBordersCoreEdgeFilter coreEdgeFilter) {
                //查询中没有指定任何国家，但 lmset 中有
                if (queryCountries.isEmpty())
                    return false;
                return queryCountries.containsAll(Arrays.stream(coreEdgeFilter.getAvoidCountries()).boxed().collect(Collectors.toList()));
            }
        }

        return true;
    }

    /**
     * 检查 LMSet 中指定的避让要素是否为查询中所请求避让要素的子集
     *
     * @return 若该 lmset 可用于计算该查询则返回 true
     */
    private boolean isAvoidFeature(int avoidable) {
        for (EdgeFilter edgeFilter : filters) {
            //每个 EdgeFilterSequence 中只有一个 AvoidFeaturesCoreEdgeFilter
            if (edgeFilter instanceof AvoidFeaturesCoreEdgeFilter coreEdgeFilter) {
                //一些位运算技巧，用于判断存储的位是否为查询位的子集（而非反过来）
                int reverseQueryFeatures = Integer.MAX_VALUE ^ avoidable;
                int filterFeatures = coreEdgeFilter.getAvoidFeatures();
                return (reverseQueryFeatures & filterFeatures) == 0;
            }
        }
        return true;
    }

}
