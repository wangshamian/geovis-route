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
package org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import org.locationtech.jts.geom.*;

import java.io.Serializable;

public class AvoidAreasEdgeFilter implements EdgeFilter {

    private Envelope env;
    private final Polygon[] polys;
    private DefaultCoordinateSequence coordSequence;
    private final GeometryFactory geomFactory = new GeometryFactory();

    /**
     * 创建一个边过滤器，接受指定车辆的两个方向。
     */
    public AvoidAreasEdgeFilter(Polygon[] polys) {
        this.polys = polys;

        if (polys != null && polys.length > 0) {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;

            for (int i = 0; i < polys.length; i++) {
                Polygon poly = polys[i];
                Envelope internal = poly.getEnvelopeInternal();
                if (internal.getMinX() < minX)
                    minX = internal.getMinX();
                if (internal.getMinY() < minY)
                    minY = internal.getMinY();
                if (internal.getMaxX() > maxX)
                    maxX = internal.getMaxX();
                if (internal.getMaxY() > maxY)
                    maxY = internal.getMaxY();
            }

            env = new Envelope(minX, maxX, minY, maxY);

            coordSequence = new DefaultCoordinateSequence(new Coordinate[1], 1);
        }
    }

    @Override
    public final boolean accept(EdgeIteratorState iter) {
        if (env == null)
            return true;

        boolean inEnv = false;
        PointList pl = iter.fetchWayGeometry(FetchMode.ALL);
        int size = pl.size();

        double eMinX = Double.MAX_VALUE;
        double eMinY = Double.MAX_VALUE;
        double eMaxX = Double.MIN_VALUE;
        double eMaxY = Double.MIN_VALUE;

        for (int j = 0; j < pl.size(); j++) {
            double x = pl.getLon(j);
            double y = pl.getLat(j);
            if (env.contains(x, y)) {
                inEnv = true;
                break;
            }

            if (x < eMinX)
                eMinX = x;
            if (y < eMinY)
                eMinY = y;
            if (x > eMaxX)
                eMaxX = x;
            if (y > eMaxY)
                eMaxY = y;
        }

        if (inEnv || !(eMinX > env.getMaxX() || eMaxX < env.getMinX() || eMinY > env.getMaxY() || eMaxY < env.getMinY())) {
            // 必须重置坐标序列，否则由于某种原因该边的包围盒会出错
            coordSequence = new DefaultCoordinateSequence(new Coordinate[1], 1);
            if (size >= 2) {
                // 如有需要则调整序列大小
                coordSequence.resize(size);

                for (int j = 0; j < size; j++) {
                    double x = pl.getLon(j);
                    double y = pl.getLat(j);
                    Coordinate c = coordSequence.getCoordinate(j);

                    if (c == null) {
                        c = new Coordinate(x, y);
                        coordSequence.setCoordinate(j, c);
                    } else {
                        c.x = x;
                        c.y = y;
                    }
                }

                LineString ls = geomFactory.createLineString(coordSequence);

                for (int i = 0; i < polys.length; i++) {
                    Polygon poly = polys[i];
                    if (poly.contains(ls) || ls.crosses(poly)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Geometry 默认使用的 CoordinateSequence 实现。在该实现中，
     * #toArray 和 #get 返回的 Coordinate 是实时的，
     * 修改它们的调用方实际上修改的是
     * DefaultCoordinateSequence 的底层数据。
     *
     * @version 1.7
     */
    static class DefaultCoordinateSequence implements CoordinateSequence, Serializable {
        //由 Markus Schaber [schabios@logi-track.com] 于 2004-03-26 贡献
        private static final long serialVersionUID = -915438501601840650L;
        private Coordinate[] coordinates;
        private int size;

        /**
         * 基于给定数组构造 DefaultCoordinateSequence（数组不会被复制）。
         *
         * @param coordinates 将被引用的坐标数组。
         */
        public DefaultCoordinateSequence(Coordinate[] coordinates, int size) {
            if (coordinates == null) {
                throw new IllegalArgumentException("Null coordinate");
            }
            this.coordinates = coordinates;
            this.size = size;
        }

        /**
         * @see org.locationtech.jts.geom.CoordinateSequence#getDimension()
         */
        public int getDimension() {
            return 3;
        }

        /**
         * 获取索引 i 处的 Coordinate。
         *
         * @param i 坐标的索引
         * @return 请求的 Coordinate 实例
         */
        public Coordinate getCoordinate(int i) {
            return coordinates[i];
        }

        public void setCoordinate(int i, Coordinate c) {
            coordinates[i] = c;
        }

        /**
         * 获取索引 i 处 Coordinate 的副本。
         *
         * @param i 坐标的索引
         * @return 请求的 Coordinate 的副本
         */
        public Coordinate getCoordinateCopy(int i) {
            return new Coordinate(coordinates[i]);
        }

        /**
         * @see org.locationtech.jts.geom.CoordinateSequence#getX(int)
         */
        public void getCoordinate(int index, Coordinate coord) {
            coord.x = coordinates[index].x;
            coord.y = coordinates[index].y;
        }

        /**
         * @see org.locationtech.jts.geom.CoordinateSequence#getX(int)
         */
        public double getX(int index) {
            return coordinates[index].x;
        }

        /**
         * @see org.locationtech.jts.geom.CoordinateSequence#getY(int)
         */
        public double getY(int index) {
            return coordinates[index].y;
        }

        /**
         * @see org.locationtech.jts.geom.CoordinateSequence#getOrdinate(int, int)
         */
        public double getOrdinate(int index, int ordinateIndex) {
            switch (ordinateIndex) {
                case CoordinateSequence.X:
                    return coordinates[index].x;
                case CoordinateSequence.Y:
                    return coordinates[index].y;
                case CoordinateSequence.Z:
                    return coordinates[index].z;
                default:
                    break;
            }
            return Double.NaN;
        }

        /**
         * @see org.locationtech.jts.geom.CoordinateSequence#setOrdinate(int, int, double)
         */
        public void setOrdinate(int index, int ordinateIndex, double value) {
            switch (ordinateIndex) {
                case CoordinateSequence.X -> coordinates[index].x = value;
                case CoordinateSequence.Y -> coordinates[index].y = value;
                case CoordinateSequence.Z -> coordinates[index].z = value;
                default -> {
                }
            }
        }

        /**
         * 创建该对象的深拷贝
         *
         * @return 深拷贝
         */
        public CoordinateSequence copy() {
            Coordinate[] cloneCoordinates = new Coordinate[size];
            for (int i = 0; i < coordinates.length; i++) {
                cloneCoordinates[i] = coordinates[i].copy();
            }
            return new DefaultCoordinateSequence(cloneCoordinates, size);
        }

        @Override
        @Deprecated
        public Object clone() {
            return copy();
        }

        /**
         * 返回坐标序列的大小
         *
         * @return 坐标的数量
         */
        public int size() {
            return size;
        }

        public void resize(int size) {
            if (size > this.size) {
                coordinates = new Coordinate[size];
            }

            this.size = size;
        }

        /**
         * 该方法暴露内部的 Coordinate 对象数组
         *
         * @return Coordinate[] 数组。
         */
        public Coordinate[] toCoordinateArray() {
            return coordinates;
        }

        public Envelope expandEnvelope(Envelope env) {
            for (int i = 0; i < coordinates.length; i++) {
                env.expandToInclude(coordinates[i]);
            }
            return env;
        }

        /**
         * 返回坐标数组的字符串表示
         *
         * @return 字符串
         */
        public String toString() {
            if (coordinates.length > 0) {
                StringBuilder strBuf = new StringBuilder(17 * coordinates.length);
                strBuf.append('(');
                strBuf.append(coordinates[0]);
                for (int i = 1; i < coordinates.length; i++) {
                    strBuf.append(", ");
                    strBuf.append(coordinates[i]);
                }
                strBuf.append(')');
                return strBuf.toString();
            } else {
                return "()";
            }
        }
    }
}
