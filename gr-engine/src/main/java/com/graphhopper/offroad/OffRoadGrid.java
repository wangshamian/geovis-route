package com.graphhopper.offroad;

import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

import java.util.List;

public class OffRoadGrid {
    protected final int x;
    protected final int y;
    protected final GHPoint center;
    protected final double slope;
    /**
     * 网格中心高程（米），用于区分上坡/下坡的通行速度衰减。
     */
    protected final double elevation;
    protected final List<GHPoint> coordinates;
    protected String id;
    protected String parentId;
    protected double h = 0;
    protected double g = 0;
    protected double f = 0;
    protected boolean isInCloseList;
    protected boolean isInOpenList;
    /**
     * 网格中心附近公路的通行速度（km/h），0 表示该网格不在公路上。
     */
    protected double roadSpeedKmh = 0;

    public OffRoadGrid(int x, int y, double slope, double elevation, GHPoint center, List<GHPoint> coordinates) {
        this.x = x;
        this.y = y;
        this.id = x + "-" + y;
        this.slope = slope;
        this.elevation = elevation;
        this.center = center;
        this.coordinates = coordinates;
    }
}
