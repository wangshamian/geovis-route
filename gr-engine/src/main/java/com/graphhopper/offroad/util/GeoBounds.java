package com.graphhopper.offroad.util;

import com.graphhopper.offroad.OffRoadGrid;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据经纬度和距离计算新的经纬度范围
 */
public class GeoBounds {
    public double minLon;
    public double maxLon;
    public double minLat;
    public double maxLat;
    private static final double EARTH_RADIUS = 6378137.0; // 地球半径（米）

    public GeoBounds(double minLon, double maxLon, double minLat, double maxLat) {
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
    }

    /**
     * 扩大现有范围（按比例）
     */
    public GeoBounds expandBoundsByRatio(double ratio) {
        double centerLng = (this.minLon + this.maxLon) / 2;
        double centerLat = (this.minLat + this.maxLat) / 2;

        double lngRange = this.maxLon - this.minLon;
        double latRange = this.maxLat - this.minLat;

        double newLngRange = lngRange * ratio;
        double newLatRange = latRange * ratio;

        return new GeoBounds(centerLng - newLngRange / 2, centerLng + newLngRange / 2, centerLat - newLatRange / 2, centerLat + newLatRange / 2);
    }

    /**
     * 获取宽度
     */
    public double getWidth() {
        return maxLon - minLon;
    }

    /**
     * 获取高度
     */
    public double getHeight() {
        return maxLat - minLat;
    }
}
