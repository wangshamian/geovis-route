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
     * 按距离（米）向四周扩展范围，用于构造走廊式搜索边界。
     */
    public GeoBounds expandBoundsByMeters(double meters) {
        double centerLat = (this.minLat + this.maxLat) / 2;
        // 经度方向 1 度对应的米数随纬度收缩，用平均纬度换算
        double lonScale = 111320.0 * Math.max(0.01, Math.cos(Math.toRadians(centerLat)));
        double latDelta = meters / 111320.0;
        double lonDelta = meters / lonScale;
        return new GeoBounds(minLon - lonDelta, maxLon + lonDelta, minLat - latDelta, maxLat + latDelta);
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
