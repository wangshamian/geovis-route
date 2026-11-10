package org.heigit.geovisroute.routing.graphhopper.extensions.reader.traffic;

import com.graphhopper.util.DistanceCalcEarth;
import org.apache.log4j.Logger;
import org.geotools.api.feature.Property;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import java.io.InvalidObjectException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class TrafficLink {
    private static final Logger LOGGER = Logger.getLogger(TrafficLink.class);

    private final int linkId;
    private double linkLength;
    private boolean isTeardrop;

    private LineString linkGeometry;
    private final TrafficLinkMetadata trafficLinkMetadata;
    private final EnumMap<TrafficEnums.WeekDay, Integer> trafficPatternIdsFrom;
    private final EnumMap<TrafficEnums.WeekDay, Integer> trafficPatternIdsTo;

    /**
     * 构造用于处理 link 交通数据的 TrafficLink 对象。
     *
     * @param linkId            link 的 ID
     * @param linkGeometry      表示 link 的几何
     * @param properties        link 的属性
     * @param distanceCalcEarth 已初始化的 {@link DistanceCalcEarth} 对象，可方便地复用于计算长度。
     * @throws InvalidObjectException 当几何无效时提供详细信息。
     */
    public TrafficLink(int linkId, Geometry linkGeometry, Collection<Property> properties, DistanceCalcEarth distanceCalcEarth) throws InvalidObjectException {
        this.linkId = linkId;

        this.trafficPatternIdsFrom = new EnumMap<>(TrafficEnums.WeekDay.class);
        this.trafficPatternIdsTo = new EnumMap<>(TrafficEnums.WeekDay.class);

        this.setLinkGeometry(linkGeometry);
        this.setLinkLength(distanceCalcEarth);

        trafficLinkMetadata = new TrafficLinkMetadata(properties);

    }

    private void setLinkLength(DistanceCalcEarth dc) {
        double temporaryLength = 0;

        if (this.getLinkGeometry() != null) {
            LineString ls = this.getLinkGeometry();
            int nPoints = ls.getNumPoints();

            if (nPoints > 1) {
                Coordinate c = ls.getCoordinateN(0);
                double x0 = c.x;
                double y0 = c.y;
                for (int i = 1; i < ls.getNumPoints(); i++) {
                    c = ls.getCoordinateN(i);

                    temporaryLength += dc.calcDist(y0, x0, c.y, c.x);
                    x0 = c.x;
                    y0 = c.y;
                }
            }
        }
        this.linkLength = temporaryLength;
    }

    public double getLinkLength() {
        return this.linkLength;
    }

    public int getLinkId() {
        return this.linkId;
    }

    /**
     * link 几何的默认方向为 from，参见 isfromGeometry。
     * 当检测到 teardrop 时，该几何会在 isPotentialTrafficSegment 中被完全忽略。
     *
     * @param linkGeometry 要赋给 link 线串的几何
     * @throws InvalidObjectException
     */
    private void setLinkGeometry(Geometry linkGeometry) throws InvalidObjectException {
        GeometryFactory gf = new GeometryFactory();
        if (linkGeometry.getGeometryType().equals("LineString")) {
            LineString geometry = gf.createLineString(linkGeometry.getCoordinates());
            isTeardrop = checkTearDrop(geometry);
            this.linkGeometry = geometry;
        } else {
            LOGGER.error("Invalid geometry - " + linkGeometry.getGeometryType());
            throw new InvalidObjectException("Invalid geometry for linkId " + linkId);
        }
    }

    public LineString getLinkGeometry() {
        return this.linkGeometry;
    }

    public void setTrafficPatternId(TrafficEnums.TravelDirection travelDirection, TrafficEnums.WeekDay weekDay, Integer trafficPatternId) {
        if (travelDirection == TrafficEnums.TravelDirection.TO) {
            this.trafficPatternIdsTo.put(weekDay, trafficPatternId);
        } else {
            this.trafficPatternIdsFrom.put(weekDay, trafficPatternId);
        }
    }

    public Map<TrafficEnums.WeekDay, Integer> getTrafficPatternIds(TrafficEnums.TravelDirection travelDirection) {
        if (travelDirection == TrafficEnums.TravelDirection.TO) {
            return this.trafficPatternIdsTo;
        } else {
            return this.trafficPatternIdsFrom;
        }
    }

    public boolean isPotentialTrafficSegment() {
        if (isTeardrop) return false;
        if (trafficPatternIdsTo.isEmpty() && trafficPatternIdsFrom.isEmpty()) return false;
        if (trafficLinkMetadata.isFerry()) return false;
        if (trafficLinkMetadata.isRoundAbout()) return false;
        return trafficLinkMetadata.functionalClass() != TrafficEnums.FunctionalClass.CLASS5;
    }

    public boolean isBothDirections() {
        return trafficLinkMetadata.getTravelDirection() == TrafficEnums.LinkTravelDirection.BOTH;
    }

    public int getFunctionalClass() {
        return this.trafficLinkMetadata.getFunctionalClassWithRamp();
    }

    private boolean checkTearDrop(LineString lineString) {
        double coordinateFirstX = lineString.getCoordinateN(0).x;
        double coordinateFirstY = lineString.getCoordinateN(0).y;
        double coordinateLastX = lineString.getCoordinateN(lineString.getCoordinates().length - 1).x;
        double coordinateLastY = lineString.getCoordinateN(lineString.getCoordinates().length - 1).y;

        if (coordinateFirstY < coordinateLastY) {
            // 若第一个坐标的纬度更小，则以它为参考。这是最常见的情况！
            return false;
        } else if (coordinateFirstY > coordinateLastY) {
            // 若最后一个坐标的纬度更小，则以它为参考。
            return false;
        } else // 若纬度相同但第一个坐标的经度更小，则以它为参考。
            // 具有相同坐标的 teardrop 节点。来自 Here 的道路不应出现这种情况！
            if (coordinateFirstX < coordinateLastX) {
                // 若纬度相同但第一个坐标的经度更小，则以它为参考。
                // 这表示水平线 >------>
                return false;
            } else return !(coordinateFirstX > coordinateLastX);
    }

    public Geometry getToGeometry() {
        return linkGeometry.reverse();
    }

    public Geometry getFromGeometry() {
        return linkGeometry;
    }

    public boolean isOnlyFromDirection() {
        return trafficLinkMetadata.getTravelDirection() == TrafficEnums.LinkTravelDirection.FROM;
    }
}
