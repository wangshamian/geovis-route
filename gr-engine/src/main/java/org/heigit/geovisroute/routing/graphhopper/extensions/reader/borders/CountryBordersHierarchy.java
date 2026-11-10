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

import org.locationtech.jts.geom.Coordinate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于以层次结构存储国家边界多边形的对象。
 */
public class CountryBordersHierarchy implements Serializable {
    private double minLat = 180f;
    private double minLon = 180f;
    private double maxLat = -180f;
    private double maxLon = -180f;
    private final ArrayList<CountryBordersPolygon> polygons = new ArrayList<>();

    /**
     * 向层次结构中添加一个边界多边形，并更新该层次结构的范围。
     *
     * @param cp
     */
    public void add(CountryBordersPolygon cp) {
        this.polygons.add(cp);
        // 更新包围盒
        double[] bb = cp.getBBox();
        if (bb[0] < minLon) minLon = bb[0];
        if (bb[1] > maxLon) maxLon = bb[1];
        if (bb[2] < minLat) minLat = bb[2];
        if (bb[3] > maxLat) maxLat = bb[3];
    }

    /**
     * 检查给定坐标是否位于该层次结构的包围盒内。该包围盒围绕层次结构对象所包含的
     * 所有国家多边形
     *
     * @param c 要查询的坐标
     * @return
     */
    public boolean inBbox(Coordinate c) {
        return !(c.x <= minLon || c.x >= maxLon || c.y <= minLat || c.y >= maxLat);
    }


    public double[] getBBox() {
        return new double[]{minLon, maxLon, minLat, maxLat};
    }


    public List<CountryBordersPolygon> getPolygons() {
        return polygons;
    }

    /**
     * 遍历该层次结构中的国家多边形，找出给定坐标所在的多边形
     *
     * @param c 要查询的坐标
     * @return 包含该坐标的多边形对象列表
     */
    public List<CountryBordersPolygon> getContainingPolygons(Coordinate c) {

        ArrayList<CountryBordersPolygon> containing = new ArrayList<>();
        if (!Double.isNaN(c.x) && !Double.isNaN(c.y) && inBbox(c)) {
            for (CountryBordersPolygon cbp : polygons) {
                if (cbp.inBbox(c)) {
                    containing.add(cbp);
                }
            }
        }
        return containing;
    }
}
