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
package org.heigit.geovisroute.isochrones.builders.fast;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.ev.Subnetwork;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.*;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint3D;
import org.heigit.geovisroute.isochrones.builders.AbstractIsochroneMapBuilder;
import org.heigit.geovisroute.util.ProfileTools;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.common.TravelRangeType;
import org.heigit.geovisroute.exceptions.InternalServerException;
import org.heigit.geovisroute.fastisochrones.FastIsochroneAlgorithm;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.isochrones.IsochroneMap;
import org.heigit.geovisroute.isochrones.IsochroneSearchParameters;
import org.heigit.geovisroute.isochrones.IsochronesErrorCodes;
import org.heigit.geovisroute.routing.AvoidFeatureFlags;
import org.heigit.geovisroute.routing.RouteSearchContext;
import org.heigit.geovisroute.routing.graphhopper.extensions.AccessibilityMap;
import org.heigit.geovisroute.routing.graphhopper.extensions.GREdgeFilterFactory;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraphHopper;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRWeightingFactory;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.AvoidFeaturesEdgeFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;
import org.heigit.geovisroute.util.DebugUtility;

import java.util.*;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.*;
import static org.locationtech.jts.algorithm.hull.ConcaveHull.concaveHullByLength;

/**
 * 使用快速等时圈算法计算等时圈多边形。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class FastIsochroneMapBuilder extends AbstractIsochroneMapBuilder {
    private CellStorage cellStorage;
    private IsochroneNodeStorage isochroneNodeStorage;
    private QueryGraph queryGraph;
    private static final int MAX_SEGMENT_LENGTH = Integer.MAX_VALUE;
    private static final double ACTIVE_CELL_APPROXIMATION_FACTOR = 0.99;
    private static final Logger LOGGER = Logger.getLogger(FastIsochroneMapBuilder.class.getName());

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void initialize(RouteSearchContext searchContext) {
        super.initialize(searchContext);
        var fastIsochroneFactory = ((GRGraphHopper) searchContext.getGraphHopper()).getFastIsochroneFactory();
        this.cellStorage = fastIsochroneFactory.getCellStorage();
        this.isochroneNodeStorage = fastIsochroneFactory.getIsochroneNodeStorage();
    }

    public IsochroneMap compute(IsochroneSearchParameters parameters) throws Exception {
        StopWatch swTotal = null;
        StopWatch sw = null;
        if (DebugUtility.isDebug()) {
            swTotal = new StopWatch();
            swTotal.start();
            sw = new StopWatch();
            sw.start();
        }
        double maxSpeed = determineMaxSpeed();

        double meanSpeed = determineMeanSpeed(maxSpeed);

        double metersPerSecond = maxSpeed / 3.6;
        // 仅用于 reachfactor 属性
        double meanMetersPerSecond = meanSpeed / 3.6;

        GraphHopper gh = searchContext.getGraphHopper();
        EncodingManager encodingManager = gh.getEncodingManager();
        Weighting weighting = GRWeightingFactory.createIsochroneWeighting(searchContext, encodingManager);

        Coordinate loc = parameters.getLocation();

        String vehicleName = searchContext.getVehicleName();
        EdgeFilter defaultSnapFilter = new DefaultSnapFilter(weighting,
                GRWeightingFactory.getSubnetworkEncodedValue(encodingManager, vehicleName));

        GREdgeFilterFactory edgeFilterFactory = new GREdgeFilterFactory();
        EdgeFilterSequence edgeFilterSequence = getEdgeFilterSequence(edgeFilterFactory, defaultSnapFilter);
        Snap res = searchContext.getGraphHopper().getLocationIndex().findClosest(loc.y, loc.x, edgeFilterSequence);
        List<Snap> snaps = new ArrayList<>(1);
        snaps.add(res);
        //需要用于获取起点所在的单元（预处理信息，因此没有关于虚拟节点的信息）
        int nonvirtualClosestNode = res.getClosestNode();
        if (nonvirtualClosestNode == -1)
            throw new InternalServerException(IsochronesErrorCodes.UNKNOWN, "The closest node is null.");

        BaseGraph graph = searchContext.getGraphHopper().getBaseGraph();
        queryGraph = QueryGraph.create(graph, snaps);
        int from = res.getClosestNode();

        //这里会计算限制范围内的节点
        //目前仅支持基于节点的模式
        if (!(searchContext.getGraphHopper() instanceof GRGraphHopper))
            throw new IllegalStateException("Unable to run fast isochrones without GRGraphhopper");

        int nRanges = parameters.getRanges().length;
        IsochroneMap isochroneMap = null;

        for (int i = 0; i < nRanges; i++) {
            FastIsochroneAlgorithm fastIsochroneAlgorithm = new FastIsochroneAlgorithm(
                    queryGraph,
                    weighting,
                    TraversalMode.NODE_BASED,
                    cellStorage,
                    isochroneNodeStorage,
                    ((GRGraphHopper) searchContext.getGraphHopper()).getEccentricity().getEccentricityStorage(weighting),
                    ((GRGraphHopper) searchContext.getGraphHopper()).getEccentricity().getBorderNodeDistanceStorage(weighting),
                    edgeFilterSequence);
            //考虑吸附距离
//            double isolimit = parameters.getRanges()[i] - weighting.getMinWeight(res.getQueryDistance());
            double isolimit = parameters.getRanges()[i] - weighting.calcMinWeightPerDistance();
            if (isolimit <= 0)
                throw new IllegalStateException("Distance of query to snapped position is greater than isochrone limit!");

            fastIsochroneAlgorithm.calcIsochroneNodes(from, nonvirtualClosestNode, isolimit);

            Set<Geometry> isochroneGeometries = new HashSet<>();

            if (DebugUtility.isDebug()) {
                LOGGER.debug("Find edges: " + sw.stop().getSeconds());
                sw = new StopWatch();
                sw.start();
            }

            fastIsochroneAlgorithm.approximateActiveCells(ACTIVE_CELL_APPROXIMATION_FACTOR);

            if (DebugUtility.isDebug()) {
                LOGGER.debug("Approximate active cells: " + sw.stop().getSeconds());
                sw = new StopWatch();
                sw.start();
            }
            //添加所有完全可达的单元几何
            handleFullyReachableCells(isochroneGeometries, fastIsochroneAlgorithm.getFullyReachableCells());

            if (DebugUtility.isDebug()) {
                LOGGER.debug("Handle " + fastIsochroneAlgorithm.getFullyReachableCells().size() + " fully reachable cells: " + sw.stop().getSeconds());
            }

            GHPoint3D snappedPosition = res.getSnappedPoint();

            AccessibilityMap edgeMap = new AccessibilityMap(fastIsochroneAlgorithm.getStartCellMap(), snappedPosition);

            final Coordinate snappedLoc = (snappedPosition == null) ? parameters.getLocation() : new Coordinate(snappedPosition.lon, snappedPosition.lat);

            if (isochroneMap == null) isochroneMap = new IsochroneMap(parameters.getTravellerId(), snappedLoc);

            if (edgeMap.isEmpty())
                return isochroneMap;

            List<Coordinate> isoPoints = new ArrayList<>((int) (1.2 * edgeMap.getMap().size()));

            double isoValue = parameters.getRanges()[i];

            TravelRangeType isochroneType = parameters.getRangeType();

            final double maxRadius;
            double meanRadius;
            if (isochroneType == TravelRangeType.TIME) {
                maxRadius = metersPerSecond * isoValue;
                meanRadius = meanMetersPerSecond * isoValue;
            } else {
                maxRadius = isoValue;
                meanRadius = isoValue;
            }

            double smoothingDistance = convertSmoothingFactorToDistance(parameters.getSmoothingFactor(), maxRadius);

            //添加上一个等时圈区间的多边形
            addPreviousIsochronePolygon(isochroneGeometries);
            buildActiveCellsConcaveHulls(fastIsochroneAlgorithm, isochroneGeometries, snappedLoc, snappedPosition, isoValue, smoothingDistance);

            if (!isochroneGeometries.isEmpty()) {
                //对所有现有多边形做并集，以减少坐标列表
                //取消注释可查看响应中的所有几何
                //for(Geometry poly : isochroneGeometries)
                //  isochroneMap.addIsochrone(new Isochrone(poly, isoValue, meanRadius));
                Geometry preprocessedGeometry = combineGeometries(isochroneGeometries);

                StopWatch finalConcaveHullStopWatch = new StopWatch();
                if (DebugUtility.isDebug())
                    finalConcaveHullStopWatch.start();
                isoPoints.addAll(createCoordinateListFromGeometry(preprocessedGeometry, smoothingDistance));
                GeometryCollection points = buildIsochrone(new AccessibilityMap(new GHIntObjectHashMap<>(0), snappedPosition), isoPoints, isoValue, smoothingDistance);
                addIsochrone(isochroneMap, points, isoValue, meanRadius, smoothingDistance);
                if (DebugUtility.isDebug()) {
                    LOGGER.debug("Build final concave hull from " + points.getNumGeometries() + " points: " + finalConcaveHullStopWatch.stop().getSeconds());
                }
            }
        }

        if (DebugUtility.isDebug())
            LOGGER.debug("Total time: " + swTotal.stop().getSeconds());

        return isochroneMap;
    }

    private EdgeFilterSequence getEdgeFilterSequence(GREdgeFilterFactory edgeFilterFactory, EdgeFilter prependFilter) throws Exception {
        EdgeFilterSequence edgeFilterSequence = new EdgeFilterSequence();
        EdgeFilter edgeFilter = edgeFilterFactory.createEdgeFilter(searchContext.getProperties(), searchContext.getVehicleName(), searchContext.getGraphHopper().getBaseGraph(), prependFilter, searchContext.getGraphHopper().getEncodingManager());
        edgeFilterSequence.add(edgeFilter);
        edgeFilterSequence.add(new AvoidFeaturesEdgeFilter(AvoidFeatureFlags.FERRIES, searchContext.getGraphHopper().getBaseGraph()));
        return edgeFilterSequence;
    }

    private void buildActiveCellsConcaveHulls(FastIsochroneAlgorithm fastIsochroneAlgorithm, Set<Geometry> isochroneGeometries, Coordinate snappedLoc, GHPoint3D snappedPosition, double isoValue, double smoothingDistance) {
        //分别构建所有活跃单元的凹包
        StopWatch swActiveCellSeparate = new StopWatch();
        StopWatch swActiveCellBuild = new StopWatch();

        for (Map.Entry<Integer, IntObjectMap<SPTEntry>> activeCell : fastIsochroneAlgorithm.getActiveCellMaps().entrySet()) {
            swActiveCellSeparate.start();
            //查找活跃单元中不连通的子单元，以避免几何问题
            List<GHIntObjectHashMap<SPTEntry>> disconnectedActiveCells = separateDisconnected(activeCell.getValue());

            swActiveCellSeparate.stop();
            swActiveCellBuild.start();
            boolean largestSubCellProcessed = false;
            for (GHIntObjectHashMap<SPTEntry> splitMap : disconnectedActiveCells) {
                if (largestSubCellProcessed && splitMap.size() < getMinCellNodesNumber())
                    continue;
                largestSubCellProcessed = true;
                GeometryCollection points = buildIsochrone(new AccessibilityMap(splitMap, snappedPosition), new ArrayList<>(), isoValue, smoothingDistance);
                createPolyFromPoints(isochroneGeometries, points, smoothingDistance);
            }
            swActiveCellBuild.stop();
        }
        if (DebugUtility.isDebug()) {
            LOGGER.debug("Separate disconnected: " + swActiveCellSeparate.stop().getSeconds());
            LOGGER.debug("Build " + fastIsochroneAlgorithm.getActiveCellMaps().size() + " active cells: " + swActiveCellBuild.stop().getSeconds());
        }
    }

    private List<Coordinate> createCoordinateListFromGeometry(Geometry preprocessedGeometry, double minSplitLength) {
        List<Coordinate> contourCoords = new ArrayList<>();

        for (int j = 0; j < preprocessedGeometry.getNumGeometries(); j++) {
            Geometry geometry = preprocessedGeometry.getGeometryN(j);

            if (geometry instanceof Polygon poly) {
                List<Coordinate> ringCoords = createCoordinateListFromPolygon(poly);
                contourCoords.addAll(ringCoords);

                double lat0 = ringCoords.get(ringCoords.size() - 1).y;
                double lon0 = ringCoords.get(ringCoords.size() - 1).x;
                double lat1;
                double lon1;
                for (int i = 0; i < ringCoords.size(); i++) {
                    lat1 = ringCoords.get(i).y;
                    lon1 = ringCoords.get(i).x;
                    splitLineSegment(lat0, lon0, lat1, lon1, contourCoords, minSplitLength, MAX_SEGMENT_LENGTH);
                    lon0 = lon1;
                    lat0 = lat1;
                }
            }
            else {
                contourCoords.addAll(Arrays.asList(geometry.getCoordinates()));
            }
        }

        return contourCoords;
    }

    private Geometry combineGeometries(Set<Geometry> isochroneGeometries) {
        StopWatch unaryUnionStopWatch = new StopWatch();
        if (DebugUtility.isDebug())
            unaryUnionStopWatch.start();
        Geometry preprocessedGeometry = UnaryUnionOp.union(isochroneGeometries);
        if (DebugUtility.isDebug()) {
            LOGGER.debug("Union of geometries: " + unaryUnionStopWatch.stop().getSeconds());
        }
        return preprocessedGeometry;
    }

    private void addPreviousIsochronePolygon(Set<Geometry> isochroneGeometries) {
        if (previousIsochronePolygon != null)
            isochroneGeometries.add(previousIsochronePolygon);
    }

    private void createPolyFromPoints(Set<Geometry> isochroneGeometries, GeometryCollection points, double smoothingDistance) {
        if (points.isEmpty())
            return;

        try {
            Geometry concaveHull = concaveHullByLength(points, smoothingDistance);
            if (concaveHull instanceof Polygon && concaveHull.isValid() && !concaveHull.isEmpty())
                isochroneGeometries.add(concaveHull);
        } catch (Exception e) {
            if (isLogEnabled()) LOGGER.debug(e.getMessage());
        }
    }

    private GeometryCollection buildIsochrone(AccessibilityMap edgeMap, List<Coordinate> points,
                                              double isolineCost, double minSplitLength) {
        IntObjectMap<SPTEntry> map = edgeMap.getMap();

        BaseGraph graph = searchContext.getGraphHopper().getBaseGraph();

        int maxNodeId = graph.getNodes() - 1;
        int maxEdgeId = graph.getEdges() - 1;

        double bufferSize = 0.0018;

        boolean useHighDetail = map.size() < 1000;

        if (useHighDetail) {
            bufferSize = 0.0009;
        }

        for (IntObjectCursor<SPTEntry> entry : map) {
            SPTEntry goalEdge = entry.value;
            int edgeId = goalEdge.originalEdge;
            int nodeId = goalEdge.adjNode;

            if (edgeId == -1 || nodeId == -1 || nodeId > maxNodeId || edgeId > maxEdgeId)
                continue;

            float maxCost = (float) goalEdge.weight;
            float minCost = (float) goalEdge.parent.weight;

            EdgeIteratorState iter = graph.getEdgeIteratorState(edgeId, nodeId);

            // 完全位于等时圈内部的边
            if (isolineCost >= maxCost) {
                // 这会检查死胡同边，但在小区域中我们需要包含这些边，
                // 才能提供真实的结果
                if (goalEdge.edge != -2 || useHighDetail) {
                    addBufferedEdgeGeometry(points, minSplitLength, iter, true, goalEdge, bufferSize);
                }
            } else {
                if (minCost < isolineCost && maxCost >= isolineCost) {
                    addBorderEdgeGeometry(points, isolineCost, minSplitLength, iter, maxCost, minCost, bufferSize);
                }
            }
        }
        Geometry[] geometries = new Geometry[points.size()];

        for (int i = 0; i < points.size(); ++i) {
            Coordinate c = points.get(i);
            geometries[i] = geometryFactory.createPoint(c);
        }

        return new GeometryCollection(geometries, geometryFactory);
    }

    private void handleFullyReachableCells(Set<Geometry> isochroneGeometries, Set<Integer> fullyReachableCells) {
        //用于调试输出
//        StringBuilder cellsPrintStatement = new StringBuilder();
//
//        if (DebugUtility.isDebug()) {
//            cellsPrintStatement.append(System.lineSeparator());
//            cellsPrintStatement.append("{" +
//                    "  \"type\": \"FeatureCollection\"," +
//                    "  \"features\": [");
//            cellsPrintStatement.append(System.lineSeparator());
//        }
        Set<Integer> reachableCellsAndSuperCells = isSupercellsEnabled() ? handleSuperCells(fullyReachableCells) : fullyReachableCells;

        for (int cellId : reachableCellsAndSuperCells) {
            addCellPolygon(cellId, isochroneGeometries);
//            if (DebugUtility.isDebug())
//                cellsPrintStatement.append(printCell(cellStorage.getCellContourOrder(cellId), cellId));
        }
//        if (DebugUtility.isDebug()) {
//            cellsPrintStatement.deleteCharAt(cellsPrintStatement.length() - 2);
//            cellsPrintStatement.append("]}");
//            cellsPrintStatement.append(System.lineSeparator());
//        }
//        LOGGER.debug(cellsPrintStatement.toString());
    }

    private Set<Integer> handleSuperCells(Set<Integer> fullyReachableCells) {
        Set<Integer> reachableCellsAndSuperCells = new HashSet<>();
        Set<Integer> reachableSuperCells = new HashSet<>();
        for (int cellId : fullyReachableCells) {
            int superCell = cellStorage.getSuperCellOfCell(cellId);
            if (superCell != -1 && fullyReachableCells.containsAll(cellStorage.getCellsOfSuperCellAsList(superCell)))
                reachableSuperCells.add(superCell);
            else {
                reachableCellsAndSuperCells.add(cellId);
            }
        }
        for (int cellId : reachableSuperCells) {
            int superCell = cellStorage.getSuperCellOfCell(cellId);
            if (superCell != -1 && reachableSuperCells.containsAll(cellStorage.getCellsOfSuperCellAsList(superCell))) {
                reachableCellsAndSuperCells.add(superCell);
            } else {
                reachableCellsAndSuperCells.add(cellId);
            }
        }
        return reachableCellsAndSuperCells;
    }

    private void addCellPolygon(int cellId, Set<Geometry> isochronePolygons) {
        List<Double> coordinates = cellStorage.getCellContourOrder(cellId);
        if (coordinates.size() % 2 != 0)
            throw new IllegalArgumentException("Coordinate list must contain equal number of lats and lons but has odd numbered size.");
        Coordinate[] cArray = new Coordinate[coordinates.size() / 2];
        //将 double 列表（lat0,lon0,lat1,lon1,...）转换为坐标数组
        for (int n = cArray.length - 1; n >= 0; n--) {
            cArray[cArray.length - 1 - n] = new Coordinate(coordinates.get(2 * n + 1).floatValue(), coordinates.get(2 * n).floatValue());
        }
        Polygon polygon = geometryFactory.createPolygon(cArray);
        if (polygon.isValid() && !polygon.isEmpty()) {
            isochronePolygons.add(polygon);
        } else
            LOGGER.debug("Poly of cell " + cellId + " is invalid at size " + cArray.length);
    }

    //调试
    private String printCell(List<Double> coordinates, int cellId) {
        if (coordinates.size() < 3)
            return "";
        StringBuilder statement = new StringBuilder();
        statement.append("{\"type\": \"Feature\",\"properties\": {\"name\": \"").append(cellId).append("\"},\"geometry\": {\"type\": \"Polygon\",\"coordinates\": [[");
        int i;
        for (i = coordinates.size() - 2; i > 0; i -= 2) {
            statement.append("[").append(String.valueOf(coordinates.get(i + 1)), 0, Math.min(8, String.valueOf(coordinates.get(i + 1)).length())).append(",").append(String.valueOf(coordinates.get(i)), 0, Math.min(8, String.valueOf(coordinates.get(i)).length())).append("],");
        }
        statement.append("[").append(String.valueOf(coordinates.get(coordinates.size() - 1)), 0, Math.min(8, String.valueOf(coordinates.get(coordinates.size() - 1)).length())).append(",").append(String.valueOf(coordinates.get(coordinates.size() - 2)), 0, Math.min(8, String.valueOf(coordinates.get(coordinates.size() - 2)).length())).append("]");

        statement.append("]]}},");
        statement.append(System.lineSeparator());
        return statement.toString();
    }

    private List<GHIntObjectHashMap<SPTEntry>> separateDisconnected(IntObjectMap<SPTEntry> map) {
        List<GHIntObjectHashMap<SPTEntry>> disconnectedCells = new ArrayList<>();
        EdgeExplorer edgeExplorer = queryGraph.createEdgeExplorer();
        Queue<Integer> queue = new ArrayDeque<>();
        IntHashSet visitedNodes = new IntHashSet(map.size());
        for (IntObjectCursor<SPTEntry> entry : map) {
            if (visitedNodes.contains(entry.key))
                continue;
            visitedNodes.add(entry.key);
            queue.offer(entry.key);

            GHIntObjectHashMap<SPTEntry> connectedCell = new GHIntObjectHashMap<>();

            while (!queue.isEmpty()) {
                int currentNode = queue.poll();
                connectedCell.put(currentNode, map.get(currentNode));
                EdgeIterator edgeIterator = edgeExplorer.setBaseNode(currentNode);

                while (edgeIterator.next()) {
                    int nextNode = edgeIterator.getAdjNode();
                    if (visitedNodes.contains(nextNode) || !map.containsKey(nextNode))
                        continue;
                    queue.offer(nextNode);
                    connectedCell.put(nextNode, map.get(nextNode));
                    visitedNodes.add(nextNode);
                }
            }
            disconnectedCells.add(connectedCell);
        }
        disconnectedCells.sort((a1, a2) -> a2.size() - a1.size());
        return disconnectedCells;
    }
}
