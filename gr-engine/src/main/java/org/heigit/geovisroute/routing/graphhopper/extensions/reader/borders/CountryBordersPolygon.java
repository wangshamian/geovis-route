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
package org.heigit.geovisroute.routing.graphhopper.extensions.reader.borders;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.*;

import java.io.InvalidObjectException;
import java.io.Serializable;

public class CountryBordersPolygon implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(CountryBordersPolygon.class);
    private final String name;
    private final MultiPolygon boundary;
    private final Geometry boundaryLine;
    private double area = 0;
    private double minLat = 180f;
    private double minLon = 180f;
    private double maxLat = -180f;
    private double maxLon = -180f;

    /**
     * 构造用于判断某条 way 是否跨越国界的 CountryBordersPolygon 对象
     *
     * @param name     国家的本地名称
     * @param boundary 表示该区域边界的几何
     * @throws InvalidObjectException
     */
    public CountryBordersPolygon(String name, Geometry boundary) throws InvalidObjectException {
        this.name = name;
        GeometryFactory gf = new GeometryFactory();

        if (boundary.getGeometryType().equals("Polygon"))
            this.boundary = gf.createMultiPolygon(new Polygon[]{(Polygon) boundary});
        else if (boundary.getGeometryType().equals("MultiPolygon"))
            this.boundary = (MultiPolygon) boundary;
        else {
            LOGGER.error("Invalid geometry - " + boundary.getGeometryType());
            throw new InvalidObjectException("Invalid geometry for boundary " + name);
        }
        this.boundaryLine = boundary.getBoundary();
        this.area = this.boundary.getArea();
        // 计算纬度和经度的范围
        Geometry bbox = boundary.getEnvelope();
        for (Coordinate c : bbox.getCoordinates()) {
            if (c.x < minLon) minLon = c.x;
            if (c.x > maxLon) maxLon = c.x;
            if (c.y < minLat) minLat = c.y;
            if (c.y > maxLat) maxLat = c.y;
        }
    }

    public double[] getBBox() {
        return new double[]{minLon, maxLon, minLat, maxLat};
    }

    public boolean shares(MultiPolygon other) {
        // 检查该国家多边形是否与传入的多边形共享边界
        // 假定只要有相交即为是
        return this.boundary.intersects(other);
    }

    public boolean crossesBoundary(LineString line) {
        // 检查给定的线串是否跨越该国家的边界
        return this.boundaryLine.intersects(line);
    }

    public String getName() {
        return this.name;
    }

    public MultiPolygon getBoundary() {
        return this.boundary;
    }

    public boolean inBbox(Coordinate c) {
        return !(c.x < minLon || c.x > maxLon || c.y < minLat || c.y > maxLat);
    }

    public boolean inArea(Coordinate c) {
        if (!Double.isNaN(c.x) && !Double.isNaN(c.y) && inBbox(c)) {
            GeometryFactory gf = new GeometryFactory();

            return boundary.contains(gf.createPoint(c));
        }

        return false;
    }

    public double getArea() {
        return this.area;
    }
}