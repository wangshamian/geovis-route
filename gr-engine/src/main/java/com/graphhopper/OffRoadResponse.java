package com.graphhopper;

import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;

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
