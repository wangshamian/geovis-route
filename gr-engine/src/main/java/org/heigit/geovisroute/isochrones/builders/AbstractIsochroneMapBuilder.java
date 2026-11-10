package org.heigit.geovisroute.isochrones.builders;

import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.util.parsers.BikeAverageSpeedParser;
import com.graphhopper.routing.util.parsers.BikeCommonAverageSpeedParser;
import com.graphhopper.routing.util.parsers.FootAverageSpeedParser;
import com.graphhopper.util.*;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.isochrones.Isochrone;
import org.heigit.geovisroute.isochrones.IsochroneMap;
import org.heigit.geovisroute.routing.RouteSearchContext;
import org.heigit.geovisroute.util.GeomUtility;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames.*;
import static org.locationtech.jts.algorithm.hull.ConcaveHull.concaveHullByLength;

public abstract class AbstractIsochroneMapBuilder implements IsochroneMapBuilder {
    private static final double MAX_SEGMENT_ANGLE = 0.18;// 以度为单位，对应 20000 米
    protected static final DistanceCalc dcFast = new DistancePlaneProjection();
    protected GeometryFactory geometryFactory;
    protected RouteSearchContext searchContext;
    private static final double DEFAULT_SMOOTHING_DISTANCE = 0.009;// 使用约 1000 米的默认长度
    protected Polygon previousIsochronePolygon = null;

    public abstract Logger getLogger();

    public void initialize(RouteSearchContext searchContext) {
        this.searchContext = searchContext;
        this.geometryFactory = new GeometryFactory();
    }

    /**
     * 将平滑因子转换为距离（可用于生成等时圈多边形的算法）。
     * 返回的距离值取决于半径和平滑因子。
     *
     * @param smoothingFactor 平滑过程中应使用的因子。数值越小产生的距离越小
     *                        （因此多边形可能更精细）
     * @param maxRadius       等时圈的最大半径（单位：米）
     * @return
     */
    protected double convertSmoothingFactorToDistance(float smoothingFactor, double maxRadius) {
        final double MINIMUM_DISTANCE = 0.006;

        if (smoothingFactor == -1) {
            // 没有用户定义的平滑因子，因此使用默认值

            // 对于较短的等时圈，我们希望使用更小的最小距离，否则结果会不准确
            if (maxRadius < 5000)
                return MINIMUM_DISTANCE;

            return DEFAULT_SMOOTHING_DISTANCE;
        }

        double intervalDegrees = GeomUtility.metresToDegrees(maxRadius);
        double maxLength = (intervalDegrees / 100f) * smoothingFactor;

        if (maxLength < MINIMUM_DISTANCE)
            maxLength = MINIMUM_DISTANCE;
        return maxLength;
    }

    protected void addPoint(List<Coordinate> points, double lon, double lat) {
        Coordinate p = new Coordinate(lon, lat);
        points.add(p);
    }

    protected void addBufferPoints(List<Coordinate> points, double lon0, double lat0, double lon1,
                                 double lat1, boolean addLast, double bufferSize) {
        double dx = (lon0 - lon1);
        double dy = (lat0 - lat1);
        double normLength = Math.sqrt((dx * dx) + (dy * dy));
        double scale = bufferSize / normLength;

        double dx2 = -dy * scale;
        double dy2 = dx * scale;

        addPoint(points, lon0 + dx2, lat0 + dy2);
        addPoint(points, lon0 - dx2, lat0 - dy2);

        // 如果两点相距过远，则添加一个中间点
        if (normLength > 2 * bufferSize) {
            addPoint(points, (lon0 + lon1) / 2.0 + dx2, (lat0 + lat1) / 2.0 + dy2);
            addPoint(points, (lon0 + lon1) / 2.0 - dx2, (lat0 + lat1) / 2.0 - dy2);
        }

        if (addLast) {
            addPoint(points, lon1 + dx2, lat1 + dy2);
            addPoint(points, lon1 - dx2, lat1 - dy2);
        }
    }

    protected void addPointsFromEdge(List<Coordinate> points, double isolineCost, float maxCost, float minCost, double bufferSize, double edgeDist, List<Coordinate> pl) {
        double edgeCost = maxCost - minCost;
        double costPerMeter = edgeCost / edgeDist;
        double distPolyline = 0.0;

        double lat0 = pl.get(0).y;
        double lon0 = pl.get(0).x;
        double lat1;
        double lon1;

        for (int i = 1; i < pl.size(); ++i) {
            lat1 = pl.get(i).y;
            lon1 = pl.get(i).x;

            distPolyline += dcFast.calcDist(lat0, lon0, lat1, lon1);

            double distCost = minCost + distPolyline * costPerMeter;
            if (distCost >= isolineCost) {
                double segLength = (1 - (distCost - isolineCost) / edgeCost);
                double lon2 = lon0 + segLength * (lon1 - lon0);
                double lat2 = lat0 + segLength * (lat1 - lat0);

                addBufferPoints(points, lon0, lat0, lon2, lat2, true, bufferSize);

                break;
            } else {
                addBufferPoints(points, lon0, lat0, lon1, lat1, false, bufferSize);
            }

            lat0 = lat1;
            lon0 = lon1;
        }
    }

    /**
     * 当两点之间的距离超过限制时，在两点之间分割线段
     *
     * @param lat0   第一个点的纬度
     * @param lon0   第一个点的经度
     * @param lat1   第二个点的纬度
     * @param lon1   第二个点的经度
     * @param minlim 超过此限制时边将被分割（单位：米）
     * @param maxlim 超过此限制时边将不再被分割（单位：米）
     */
    protected void splitLineSegment(double lat0, double lon0, double lat1, double lon1, List<Coordinate> points, double minlim, double maxlim) {
        double dx = (lon1 - lon0);
        double dy = (lat1 - lat0);
        double normLength = Math.sqrt((dx * dx) + (dy * dy));

        if (minlim < normLength && normLength < maxlim) {
            int n = (int) Math.ceil(normLength / minlim);
            double dx2 = dx / n;
            double dy2 = dy / n;
            for (int i = 1; i < n; i++) {
                addPoint(points, lon0 + i * dx2, lat0 + i * dy2);
            }
        }
    }

    protected List<Coordinate> edgeToPoints(EdgeIteratorState iter, double minSplitLength) {
        // 始终使用 mode=3，因为其他模式无法提供正确结果
        PointList pl = iter.fetchWayGeometry(FetchMode.ALL);

        List<Coordinate> points = new ArrayList<>(2 * pl.size());

        if (!pl.isEmpty()) {
            double lat0 = pl.getLat(0);
            double lon0 = pl.getLon(0);
            double lat1;
            double lon1;
            for (int i = 1; i < pl.size(); i++) {
                lat1 = pl.getLat(i);
                lon1 = pl.getLon(i);
                addPoint(points, lon0, lat0);
                splitLineSegment(lat0, lon0, lat1, lon1, points, minSplitLength, MAX_SEGMENT_ANGLE);
                lon0 = lon1;
                lat0 = lat1;
            }
            addPoint(points, lon0, lat0);
        }

        return points;
    }

    protected void addBufferedEdgeGeometry(List<Coordinate> points, double minSplitLength, EdgeIteratorState iter, boolean detailedShape, SPTEntry goalEdge, double bufferSize) {
        List<Coordinate> pl = edgeToPoints(iter, minSplitLength);
        if (pl.isEmpty()) {
            return;
        }

        int size = pl.size();

        double lat0 = pl.get(0).y;
        double lon0 = pl.get(0).x;
        double lat1;
        double lon1;

        if (detailedShape) {
            for (int i = 1; i < size; ++i) {
                lat1 = pl.get(i).y;
                lon1 = pl.get(i).x;

                addBufferPoints(points, lon0, lat0, lon1, lat1, goalEdge.edge < 0 && i == size - 1, bufferSize);

                lon0 = lon1;
                lat0 = lat1;
            }
        } else {
            points.addAll(pl);
        }
    }

    protected void addBorderEdgeGeometry(List<Coordinate> points, double isolineCost, double minSplitLength, EdgeIteratorState iter, float maxCost, float minCost, double bufferSize) {
        List<Coordinate> pl = edgeToPoints(iter, minSplitLength);
        if (pl.isEmpty()) {
            return;
        }
        addPointsFromEdge(points, isolineCost, maxCost, minCost, bufferSize, iter.getDistance(), pl);
    }

    protected double determineMaxSpeed() {
        String vehicleName = searchContext.getVehicleName();
        double maxSpeed = 140;

        if (Objects.equals(vehicleName, GH_FOOT) || Objects.equals(vehicleName, GH_HIKE)) {
            // 在 GH FootFlagEncoder 中，最大速度被设为 15km/h，这太高了
            maxSpeed = 4;
        }

        if (Objects.equals(vehicleName, WHEELCHAIR)) {
            maxSpeed = FootAverageSpeedParser.MEAN_SPEED;
        }

        return maxSpeed;
    }

    protected double determineMeanSpeed(double maxSpeed) {
        double meanSpeed = maxSpeed;
        String vehicleName = searchContext.getVehicleName();

        if (Objects.equals(vehicleName, GH_FOOT)) {
            meanSpeed = FootAverageSpeedParser.MEAN_SPEED;
        }

        if (Objects.equals(vehicleName, GH_BIKE)) {
            meanSpeed =  BikeCommonAverageSpeedParser.PUSHING_SECTION_SPEED;
        }

        return meanSpeed;
    }

    protected List<Coordinate> createCoordinateListFromPolygon(Polygon poly) {
        List<Coordinate> ringCoordinates = new ArrayList<>(Arrays.asList(poly.getExteriorRing().getCoordinates()));
        // 移除最后一个点，因为对于闭合环它等于第一个点
        ringCoordinates.remove(ringCoordinates.size() - 1);

        return ringCoordinates;
    }

    protected void addIsochrone(IsochroneMap isochroneMap, GeometryCollection points, double isoValue, double meanRadius, double smoothingDistance) {
        if (points.isEmpty())
            return;

        StopWatch sw = new StopWatch();
        if (getLogger().isDebugEnabled()) {
            sw = new StopWatch();
            sw.start();
        }

        Geometry shellGeometry = concaveHullByLength(points, smoothingDistance);
        if (shellGeometry instanceof GeometryCollection geomColl) {
            if (geomColl.isEmpty())
                return;
        }

        Polygon poly = (Polygon) shellGeometry;
        previousIsochronePolygon = poly;

        if (getLogger().isDebugEnabled()) {
            sw.stop();
            getLogger().debug("Build shell concave hull " + sw.getSeconds());
        }

        isochroneMap.addIsochrone(new Isochrone(poly, isoValue, meanRadius));
    }
}
