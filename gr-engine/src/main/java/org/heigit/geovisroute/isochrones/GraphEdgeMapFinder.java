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
package org.heigit.geovisroute.isochrones;

import com.carrotsearch.hppc.IntObjectMap;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.ev.Subnetwork;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.*;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.shapes.GHPoint3D;
import org.heigit.geovisroute.common.TravelRangeType;
import org.heigit.geovisroute.exceptions.InternalServerException;
import org.heigit.geovisroute.routing.RouteSearchContext;
import org.heigit.geovisroute.routing.algorithms.DijkstraCostCondition;
import org.heigit.geovisroute.routing.algorithms.TDDijkstraCostCondition;
import org.heigit.geovisroute.routing.graphhopper.extensions.AccessibilityMap;
import org.heigit.geovisroute.routing.graphhopper.extensions.GREdgeFilterFactory;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRWeightingFactory;
import org.heigit.geovisroute.routing.traffic.TrafficSpeedCalculator;
import org.heigit.geovisroute.util.ProfileTools;
import org.locationtech.jts.geom.Coordinate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class GraphEdgeMapFinder {
    private GraphEdgeMapFinder() {
    }

    public static AccessibilityMap findEdgeMap(RouteSearchContext searchCntx, IsochroneSearchParameters parameters) throws Exception {
        GraphHopper gh = searchCntx.getGraphHopper();
        String vehicleName = searchCntx.getVehicleName();
        BaseGraph graph = gh.getBaseGraph();
        EncodingManager encodingManager = gh.getEncodingManager();
        Weighting weighting = GRWeightingFactory.createIsochroneWeighting(searchCntx, encodingManager);
        EdgeFilter defaultSnapFilter = new DefaultSnapFilter(weighting,
                GRWeightingFactory.getSubnetworkEncodedValue(encodingManager, vehicleName));
        GREdgeFilterFactory edgeFilterFactory = new GREdgeFilterFactory();
        EdgeFilter edgeFilter = edgeFilterFactory.createEdgeFilter(searchCntx.getProperties(), vehicleName, graph, defaultSnapFilter, encodingManager);

        Coordinate loc = parameters.getLocation();
        Snap res = gh.getLocationIndex().findClosest(loc.y, loc.x, edgeFilter);
        List<Snap> snaps = new ArrayList<>(1);
        snaps.add(res);
        QueryGraph queryGraph = QueryGraph.create(graph, snaps);

        GHPoint3D snappedPosition = res.getSnappedPoint();

        int fromId = res.getClosestNode();

        if (fromId == -1)
            throw new InternalServerException(IsochronesErrorCodes.UNKNOWN, "The closest node is null.");

        if (parameters.isTimeDependent()) {
            return calculateTimeDependentAccessibilityMap(parameters, vehicleName, graph, edgeFilter, queryGraph, snappedPosition, fromId, weighting);
        } else {
            // 重要：它只能配合 TraversalMode.NODE_BASED 使用。
            DijkstraCostCondition dijkstraAlg = new DijkstraCostCondition(queryGraph, weighting, parameters.getMaximumRange(), parameters.getReverseDirection(),
                    TraversalMode.NODE_BASED);
            dijkstraAlg.setEdgeFilter(edgeFilter);
            dijkstraAlg.calcPath(fromId, Integer.MIN_VALUE);

            IntObjectMap<SPTEntry> edgeMap = dijkstraAlg.getMap();
            return new AccessibilityMap(edgeMap, dijkstraAlg.getCurrentEdge(), snappedPosition);
        }
    }

    /**
     * 计算最大范围可达的所有节点，并返回这些节点的映射。
     *
     * @param parameters      IsochroneSearchParameters
     * @param vehicleName     车辆名称
     * @param graph           GraphHopperStorage
     * @param edgeFilter      用于查找节点的 EdgeFilter
     * @param queryGraph      包含所有常规节点以及查询位置虚拟节点的图
     * @param snappedPosition 查询在查询图上吸附到的位置
     * @param fromId          查询的起点
     * @param weighting       要使用的权重
     * @return 包含所有可达节点的可访问性映射
     */
    private static AccessibilityMap calculateTimeDependentAccessibilityMap(IsochroneSearchParameters parameters, String vehicleName, BaseGraph graph, EdgeFilter edgeFilter, QueryGraph queryGraph, GHPoint3D snappedPosition, int fromId, Weighting weighting) {
        //对于等时圈，时间相关（目前）意味着交通相关
        TrafficSpeedCalculator trafficSpeedCalculator = new TrafficSpeedCalculator(weighting.getSpeedCalculator());
        trafficSpeedCalculator.init(graph, vehicleName);
        weighting.setSpeedCalculator(trafficSpeedCalculator);
        TDDijkstraCostCondition tdDijkstraCostCondition = new TDDijkstraCostCondition(queryGraph, weighting, parameters.getMaximumRange(), parameters.getReverseDirection(),
                TraversalMode.NODE_BASED);
        tdDijkstraCostCondition.setEdgeFilter(edgeFilter);
        //时间定义为 UTC + 1，因为最初的实现是针对德国交通数据的
        //如果修改，还需要在交通存储中相应调整
        ZonedDateTime zdt = parameters.getRouteParameters().getDeparture().atZone(trafficSpeedCalculator.getZoneId());
        trafficSpeedCalculator.setZonedDateTime(zdt);
        int toId = parameters.getReverseDirection() ? fromId : Integer.MIN_VALUE;
        fromId = parameters.getReverseDirection() ? Integer.MIN_VALUE : fromId;
        tdDijkstraCostCondition.calcPath(fromId, toId, zdt.toInstant().toEpochMilli());
        IntObjectMap<SPTEntry> edgeMap = tdDijkstraCostCondition.getMap();
        return new AccessibilityMap(edgeMap, tdDijkstraCostCondition.getCurrentEdge(), snappedPosition);
    }
}
