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
package org.heigit.geovisroute.common;

import org.heigit.geovisroute.routing.RouteSearchParameters;
import org.locationtech.jts.geom.Coordinate;

public class TravellerInfo {
    private String id = "0";
    private Coordinate location;
    private String locationType = "start"; // 取值为 start 或 destination
    private double[] ranges;
    private TravelRangeType rangeType = TravelRangeType.TIME;
    private RouteSearchParameters routeSearchParams;

    public TravellerInfo() {
        routeSearchParams = new RouteSearchParameters();
    }

    public TravellerInfo(TravellerInfo old) {
        this.id = old.id;
        this.location = old.location;
        this.locationType = old.locationType;
        this.ranges = old.ranges;
        this.rangeType = old.rangeType;
        this.routeSearchParams = old.routeSearchParams;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Coordinate getLocation() {
        return location;
    }

    public void setLocation(Coordinate location) {
        this.location = location;
    }

    public double[] getRanges() {
        return ranges;
    }

    /**
     * 返回以用户指定单位表示的半径数组。算法内部会将输入的半径转换为米，
     * 因此这里需要再转换回去。
     *
     * @param unit 返回时使用的单位
     * @return 以指定单位表示的半径数组
     */

    public double[] getRangesInUnit(String unit) {
        // 将半径从米转换为用户指定的单位
        double[] rangesInUnit = new double[ranges.length];

        if (!(unit == null || "m".equalsIgnoreCase(unit))) {
            double scale = 1.0;
            if (rangeType == TravelRangeType.DISTANCE) {
                switch (unit) {
                    case "km":
                        scale = 1 / 1000.0;
                        break;
                    case "mi":
                        scale = 1 / 1609.34;
                        break;
                    default:
                    case "m":
                        break;
                }
            }

            if (scale != 1.0) {
                for (int i = 0; i < ranges.length; i++)
                    rangesInUnit[i] = ranges[i] * scale;
                return rangesInUnit;
            }
            return ranges;
        }
        return ranges;
    }

    public void setRanges(double range, double interval) {
        int nRanges = (int) Math.ceil(range / interval);
        ranges = new double[nRanges];
        for (int i = 0; i < nRanges - 1; i++)
            ranges[i] = (i + 1) * interval;

        ranges[nRanges - 1] = range;
    }

    public void setRanges(double[] ranges) {
        this.ranges = ranges;
    }

    public double getMaximumRange() {
        double maxRange = Double.MIN_VALUE;

        for (double range : ranges) {
            if (maxRange < range)
                maxRange = range;
        }

        return maxRange;
    }

    public TravelRangeType getRangeType() {
        return rangeType;
    }

    public void setRangeType(TravelRangeType rangeType) {
        this.rangeType = rangeType;
    }

    public RouteSearchParameters getRouteSearchParameters() {
        return routeSearchParams;
    }

    public void setRouteSearchParameters(RouteSearchParameters routeSearchParams) {
        this.routeSearchParams = routeSearchParams;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
}
