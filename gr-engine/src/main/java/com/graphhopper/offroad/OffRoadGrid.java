package com.graphhopper.offroad;

import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

import java.util.List;

public class OffRoadGrid {
    protected final int x;
    protected final int y;
    protected final GHPoint center;
    protected final double slope;
    protected final List<GHPoint> coordinates;
    protected String id;
    protected String parentId;
    protected double h = 0;
    protected double g = 0;
    protected double f = 0;
    protected boolean isInCloseList;
    protected boolean isInOpenList;

    public OffRoadGrid(int x, int y, double slope,GHPoint center, List<GHPoint> coordinates) {
        this.x = x;
        this.y = y;
        this.id = x + "-" + y;
        this.slope = slope;
        this.center = center;
        this.coordinates = coordinates;
    }
}
