package com.graphhopper.offroad.util;

import com.graphhopper.reader.dem.ElevationProvider;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.util.List;

public class GeoUtils {
    public static double getDistance(GlobalCoordinates source, GlobalCoordinates target) {
        return new GeodeticCalculator().calculateGeodeticCurve(Ellipsoid.WGS84, source, target).getEllipsoidalDistance();
    }

    public static double getGridSlope(List<GlobalCoordinates> sampleCoordinates, ElevationProvider eleProvider) {
        GlobalCoordinates lowestPoint = sampleCoordinates.get(0);
        GlobalCoordinates highestPoint = sampleCoordinates.get(0);
        double maxHeight = Integer.MIN_VALUE;
        double minHeight = Integer.MAX_VALUE;

        // 计算采样点的高度
        for (int i = 0; i < sampleCoordinates.size(); i++) {
            GlobalCoordinates sampleCoordinate = sampleCoordinates.get(i);
            double height = eleProvider.getEle(sampleCoordinate.getLatitude(), sampleCoordinate.getLongitude());
            if (height > maxHeight) {
                maxHeight = height;
                highestPoint = sampleCoordinate;
            }
            if (height < minHeight) {
                minHeight = height;
                lowestPoint = sampleCoordinate;
            }
        }

        // 计算坡度
        double edge1 = GeoUtils.getDistance(highestPoint, lowestPoint);
        double edge2 = maxHeight - minHeight;
        double slope = Math.toDegrees(Math.atan(edge2 / edge1));
        return slope;
    }
}
