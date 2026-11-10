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

import com.graphhopper.routing.ch.CHPreparationHandler;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.CHConfig;
import org.heigit.geovisroute.routing.RoutingProfileCategory;
import org.heigit.geovisroute.routing.graphhopper.extensions.GraphProcessContext;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraphHopperConfig;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GRParameters.Core;

/**
 * 该类实现核心区算法装饰器，并提供与核心区
 * 预处理及其车辆 profile 相关的若干辅助方法。
 * <p>
 * 本代码基于 GraphHopper GmbH 的代码。
 *
 * @author Peter Karich
 * @author Hendrik Leuschner
 * @author Andrzej Oles
 */
public class CorePreparationHandler extends CHPreparationHandler {

    private GraphProcessContext processContext;

    public CorePreparationHandler() {
        super();
        PREPARE = Core.PREPARE;
        DISABLE = Core.DISABLE;
    }

    public void init(GRGraphHopperConfig ghConfig) {
        setPreparationThreads(ghConfig.getInt(PREPARE + "threads", getPreparationThreads()));
        setCHProfiles(ghConfig.getCoreProfiles());
        pMap = ghConfig.asPMap();
    }

    @Override
    public void createPreparations(BaseGraph baseGraph) {
        if (processContext == null)
            throw new IllegalStateException("Set processContext first!");
        super.createPreparations(baseGraph);
    }

//    protected PrepareContractionHierarchies createCHPreparation(BaseGraph baseGraph, CHConfig chConfig) {
//        EdgeFilter restrictionFilter = createCoreEdgeFilter(chConfig, baseGraph, processContext);
//        PrepareContractionHierarchies pch = new PrepareCore(baseGraph, chConfig, restrictionFilter);
//        pch.setParams(pMap);
//        return pch;
//    }

    public CorePreparationHandler setProcessContext(GraphProcessContext processContext) {
        this.processContext = processContext;
        return this;
    }

//    private EdgeFilter createCoreEdgeFilter(CHConfig chProfile, BaseGraph baseGraph, GraphProcessContext processContext) {
//        EncodingManager encodingManager = baseGraph.getEncodingManager();
//
//        int routingProfileCategory = RoutingProfileCategory.getFromEncoder(encodingManager);
//
//        /* Initialize edge filter sequence */
//        EdgeFilterSequence edgeFilterSequence = new EdgeFilterSequence();
//
//        /* Heavy vehicle filter */
//        if (encodingManager.hasEncoder(FlagEncoderNames.HEAVYVEHICLE)) {
//            edgeFilterSequence.add(new HeavyVehicleCoreEdgeFilter(gs));
//        }
//
//        /* Avoid features */
//        if ((routingProfileCategory & (RoutingProfileCategory.DRIVING | RoutingProfileCategory.CYCLING | RoutingProfileCategory.WALKING | RoutingProfileCategory.WHEELCHAIR)) != 0) {
//            edgeFilterSequence.add(new AvoidFeaturesCoreEdgeFilter(gs, routingProfileCategory));
//        }
//
//        /* Avoid borders */
//        if ((routingProfileCategory & (RoutingProfileCategory.DRIVING | RoutingProfileCategory.CYCLING)) != 0) {
//            edgeFilterSequence.add(new AvoidBordersCoreEdgeFilter(gs));
//        }
//
//        if (routingProfileCategory == RoutingProfileCategory.WHEELCHAIR) {
//            edgeFilterSequence.add(new WheelchairCoreEdgeFilter(gs));
//        }
//
//        /* Maximum speed & turn restrictions */
//        if ((routingProfileCategory & RoutingProfileCategory.DRIVING) != 0) {
//            String[] encoders = {FlagEncoderNames.CAR_ORS, FlagEncoderNames.HEAVYVEHICLE};
//            for (String encoderName : encoders) {
//                if (encodingManager.hasEncodedValue(encoderName)) {
//                    TagParser tagParser = encodingManager.getEncoder(encoderName);
//                    edgeFilterSequence.add(new MaximumSpeedCoreEdgeFilter(flagEncoder, processContext.getMaximumSpeedLowerBound()));
//                    if (chProfile.isEdgeBased() && flagEncoder.supportsTurnCosts())
//                        edgeFilterSequence.add(new TurnRestrictionsCoreEdgeFilter(flagEncoder, gs));
//                    break;
//                }
//            }
//        }
//
//        /* Conditional edges */
//        if (TimeDependentCoreEdgeFilter.hasConditionals(encodingManager)) {
//            edgeFilterSequence.add(new TimeDependentCoreEdgeFilter(gs));
//        }
//
//        if (TrafficSpeedCoreEdgeFilter.hasTrafficGraphStorage(gs)) {
//            edgeFilterSequence.add(new TrafficSpeedCoreEdgeFilter(gs));
//        }
//
//        return edgeFilterSequence;
//    }

}
