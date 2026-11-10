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

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.lm.LMConfig;
import com.graphhopper.routing.lm.LMPreparationHandler;
import com.graphhopper.routing.lm.LandmarkSuggestion;
import com.graphhopper.routing.lm.PrepareLandmarks;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.RoutingCHGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraphHopperConfig;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GraphUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GRParameters.CoreLandmark;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 该类为核心区实现 A*、地标与三角不等式（ALT）装饰器。
 * <p>
 * 本代码基于 GraphHopper GmbH 的代码。
 *
 * @author Peter Karich
 * @author Hendrik Leuschner
 * @author Andrzej Oles
 */
public class CoreLMPreparationHandler extends LMPreparationHandler {
    private final CoreLMOptions coreLMOptions = new CoreLMOptions();

    public CoreLMPreparationHandler() {
        super();

        PREPARE = CoreLandmark.PREPARE;
        DISABLE = CoreLandmark.DISABLE;
        COUNT = CoreLandmark.COUNT;
    }

    public void init(GRGraphHopperConfig ghConfig) {
        super.init(ghConfig);

        //获取需要计算的地标集合
        String coreLMSets = ghConfig.getString(CoreLandmark.LMSETS, "allow_all");
        if (!coreLMSets.isEmpty() && !coreLMSets.equalsIgnoreCase("no")) {
            List<String> tmpCoreLMSets = Arrays.asList(coreLMSets.split(";"));
            coreLMOptions.setRestrictionFilters(tmpCoreLMSets);
        }
    }

    protected void createPreparationsInternal(BaseGraph baseGraph, EncodedValueLookup encodedValueLookup, List<LandmarkSuggestion> lmSuggestions) {
        for (LMConfig lmConfig : getLMConfigs()) {
            if (!(lmConfig instanceof CoreLMConfig coreLMConfig))
                throw (new IllegalStateException("Expected instance of CoreLMConfig"));
            if (!(baseGraph instanceof BaseGraph))
                throw (new IllegalStateException("Expected instance of BaseGraph"));

            String lmConfigName = coreLMConfig.getSuperName();

            Double maximumWeight = getMaximumWeights().get(lmConfigName);
            if (maximumWeight == null)
                throw new IllegalStateException("""
                        maximumWeight cannot be null. Default should be just negative. \
                        Couldn't find \
                        """ + lmConfigName + " in " + getMaximumWeights());

            PrepareLandmarks tmpPrepareLM = new PrepareCoreLandmarks(baseGraph.getDirectory(), baseGraph,
                    encodedValueLookup,
                    coreLMConfig, getLandmarks()).
                    setLandmarkSuggestions(lmSuggestions).
                    setMaximumWeight(maximumWeight).
                    setLogDetails(getLogDetails());
            if (getMinNodes() > 1)
                tmpPrepareLM.setMinimumNodes(getMinNodes());
            addPreparation(tmpPrepareLM);
        }
    }

    /**
     * 此方法建立 CoreNode id 到 0 至 numCoreNodes 之间整数的映射以节省空间。
     * 否则我们将不得不存储大量空信息
     */
    public static HashMap<Integer, Integer> createCoreNodeIdMap(RoutingCHGraph core) {
        HashMap<Integer, Integer> coreNodeIdMap = new HashMap<>();
        int maxNode = GraphUtils.getBaseGraph(core).getNodes();
        int coreNodeLevel = maxNode;
        int index = 0;
        for (int i = 0; i < maxNode; i++) {
            if (core.getLevel(i) < coreNodeLevel)
                continue;
            coreNodeIdMap.put(i, index);
            index++;
        }
        return coreNodeIdMap;
    }

    public CoreLMOptions getCoreLMOptions() {
        return coreLMOptions;
    }

}
