package com.graphhopper;

import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wrapper containing path and error output of OffRoad.
 */
public class OffRoadResponse{
    private final List<Throwable> errors = new ArrayList<>(4);
    private PMap hintsMap = new PMap();
    private PointList offRoadPoints;
    private JsonFeature barrierFeature;
    // 坡度超限的网格数量（合并前）
    private int barrierCount;
    // 混合路由：累计用时（秒）与公路/越野里程（米）
    private long durationSeconds;
    private double roadDistance;
    private double offRoadDistance;
    // 混合路由分段明细：按公路/越野连续段切分
    private final List<Segment> segments = new ArrayList<>();
//    private String debugInfo = "";

    public OffRoadResponse() {
    }

    public void addOffRoadPoints(PointList offRoadPoints) {
        this.offRoadPoints = offRoadPoints;
    }

    public PointList getOffRoadPoints() {
        return offRoadPoints;
    }

    public void addBarrierFeature(JsonFeature barrierFeature) {
        this.barrierFeature = barrierFeature;
    }

    public JsonFeature getBarrierFeature() {
        return barrierFeature;
    }

    public void setBarrierCount(int barrierCount) {
        this.barrierCount = barrierCount;
    }

    public int getBarrierCount() {
        return barrierCount;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public double getRoadDistance() {
        return roadDistance;
    }

    public void setRoadDistance(double roadDistance) {
        this.roadDistance = roadDistance;
    }

    public double getOffRoadDistance() {
        return offRoadDistance;
    }

    public void setOffRoadDistance(double offRoadDistance) {
        this.offRoadDistance = offRoadDistance;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void addSegment(Segment segment) {
        this.segments.add(segment);
    }

    /**
     * 混合路由中的一段连续公路或越野路段。
     */
    public static class Segment {
        private final boolean onRoad;
        private final double distance;
        private final long duration;
        private final double startLat;
        private final double startLon;
        private final double endLat;
        private final double endLon;
        // 该段经过的网格中心点，供前端按段着色
        private final List<GHPoint> points;

        public Segment(boolean onRoad, double distance, long duration,
                       double startLat, double startLon, double endLat, double endLon,
                       List<GHPoint> points) {
            this.onRoad = onRoad;
            this.distance = distance;
            this.duration = duration;
            this.startLat = startLat;
            this.startLon = startLon;
            this.endLat = endLat;
            this.endLon = endLon;
            this.points = points;
        }

        public boolean isOnRoad() {
            return onRoad;
        }

        public double getDistance() {
            return distance;
        }

        public long getDuration() {
            return duration;
        }

        public double getStartLat() {
            return startLat;
        }

        public double getStartLon() {
            return startLon;
        }

        public double getEndLat() {
            return endLat;
        }

        public double getEndLon() {
            return endLon;
        }

        public List<GHPoint> getPoints() {
            return points;
        }
    }

    public void setHints(PMap hints) {
        this.hintsMap = hints;
    }

    public PMap getHints() {
        return hintsMap;
    }

    public String getHeader(String key, String defaultValue) {
        Object val = hintsMap.getObject(key.toLowerCase(Locale.ROOT), null);
        if (val instanceof List && !((List) val).isEmpty())
            return ((List) val).get(0).toString();
        return defaultValue;
    }
}
