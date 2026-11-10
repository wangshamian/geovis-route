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

import com.graphhopper.storage.BaseGraph;
import org.heigit.geovisroute.routing.AvoidFeatureFlags;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.AvoidBordersCoreEdgeFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.AvoidFeaturesCoreEdgeFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.LMEdgeFilterSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hendrik Leuschner, Andrzej Oles
 */

public class CoreLMOptions {
    List<String> coreLMSets = new ArrayList<>();
    List<LMEdgeFilterSequence> filters = new ArrayList<>();

    /**
     * 设置计算地标及其距离时使用的过滤器
     *
     * @param tmpCoreLMSets
     */
    public void setRestrictionFilters(List<String> tmpCoreLMSets) {
        this.coreLMSets = tmpCoreLMSets;
    }

    /**
     * 根据 gr-config.json 中指定的集合创建所有 LMSet 过滤器
     * 过滤器是一个 LMEdgeFilterSequence，最多由一个 AvoidFeaturesFilter 和一个 AvoidCountriesFilter 组成
     * 它们可以包含多个 avoidfeatures 和 avoidcountries
     */
    public void createRestrictionFilters(BaseGraph baseGraph) {
        //为每个 lmset 创建一个 edgefiltersequence
        for (String set : coreLMSets) {
            //现在遍历一个 lm 集合中所有以逗号分隔的值
            String[] tmpFilters = set.split(",");
            LMEdgeFilterSequence edgeFilterSequence = new LMEdgeFilterSequence();
            int feature;
            int country;
            int avoidFeatures = 0;
            List<Integer> countries = new ArrayList<>();

            for (String filterType : tmpFilters) {

                //如果是 allow_all 则不添加任何过滤器
                if (filterType.equalsIgnoreCase("allow_all")) {
                    edgeFilterSequence.appendName("allow_all");
                    break;
                }

                feature = AvoidFeatureFlags.getFromString(filterType);

                //处理规避要素
                if (feature != 0) {
                    avoidFeatures = avoidFeatures | feature;
                    edgeFilterSequence.appendName(filterType.toLowerCase());
                    continue;
                }

                //处理规避国家
                String countryPattern = "(?i)country_(\\d+)";

                if (filterType.matches(countryPattern)) {
                    try {
                        country = Integer.parseInt(filterType.replaceFirst(countryPattern, "$1"));
                    } catch (NumberFormatException e) {
                        country = 0;
                    }
                    // todo 检查国家是否有效
                    if (country != 0 && !countries.contains(country)) {
                        countries.add(country);
                        edgeFilterSequence.appendName(filterType.toLowerCase());
                    }
                }
            }

            if (avoidFeatures != 0)
                edgeFilterSequence.add(new AvoidFeaturesCoreEdgeFilter(baseGraph, -1, avoidFeatures));

            if (!countries.isEmpty()) {
                int[] avoidCountries = new int[countries.size()];
                for (int i = 0; i < countries.size(); i++) {
                    avoidCountries[i] = countries.get(i);
                }
                //每个集合只使用一个 avoidBordersCoreEdgeFilter
                edgeFilterSequence.add(new AvoidBordersCoreEdgeFilter(baseGraph, avoidCountries));
            }

            this.filters.add(edgeFilterSequence);
        }
    }

    public List<LMEdgeFilterSequence> getFilters() {
        return filters;
    }
}
