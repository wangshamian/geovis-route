/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *
 * This class is composed of code taken from JAI-tools version 1.1.0
 * http://code.google.com/p/jai-tools/
 *
 * It combines the JAI-tools classes AbstractSmoother, LineSmoother,
 * PolygonSmoother and SmootherControl. All are covered by the following
 * copyright and license:
 *
 * Copyright 2010-2011 Michael Bedward
 *
 * This file is part of jai-tools.
 *
 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.heigit.geovisroute.jts;

import org.locationtech.jts.geom.*;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * 为 {@link JTS} 工具类提供包级私有的辅助方法。
 *
 * @author Michael Bedward
 * @version $Id$
 * @source $URL$
 * @since 2.8
 */
class GeometrySmoother {

    /**
     * 定义用于控制平滑过程的方法。
     * {@code LineSmoother} 提供了默认实现，
     * 即在平滑后的分段中使用固定数量的顶点，
     * 且对参与平滑的输入顶点之间的最小距离不设下限。
     */
    private interface SmootherControl {

        /**
         * 获取输入顶点之间的最小距离，
         * 只有达到该距离的线段才会被平滑。小于该值的线段
         * 将被原样复制到输出中。
         *
         * @return 进行平滑所需的最小线段长度
         */
        double getMinLength();

        /**
         * 给定输入线段长度，返回用于平滑该线段的顶点数量。
         * 该数量包含线段端点。
         *
         * @param length 输入线段长度
         * @return 平滑后线段的顶点数量（含端点）
         */
        int getNumVertices(double length);
    }

    /**
     * 默认的平滑控制。不设最小顶点距离，
     * 且每个平滑分段使用固定数量的点。
     */
    private final SmootherControl defaultControl = new SmootherControl() {
        public double getMinLength() {
            return 0.0;
        }

        public int getNumVertices(double length) {
            return 10;
        }
    };

    /**
     * 当前的 SmootherControl 实例。
     */
    private SmootherControl control;

    /**
     * 当前使用的 {@code GeometryFactory}。
     */
    private final GeometryFactory geomFactory;

    /**
     * 用于保存给定点的插值参数的类。
     */
    private static final class InterpPoint {
        double[] t = new double[4];
        double tsum;
    }

    /**
     * 已计算的插值参数缓存
     */
    private final Map<Integer, WeakReference<InterpPoint[]>> lookup =
            new HashMap<>();

    /**
     * 创建使用给定 {@code GeometryFactory} 的新平滑器。
     *
     * @param geomFactory 用于创建平滑对象的 factory
     * @throws IllegalArgumentException 当 {@code geomFactory} 为 {@code null} 时抛出
     */
    GeometrySmoother(GeometryFactory geomFactory) {
        if (geomFactory == null) {
            throw new IllegalArgumentException("geomFactory must not be null");
        }
        this.geomFactory = geomFactory;

        this.control = defaultControl;
    }

    /**
     * 创建一个新的 {@code LineString}，它是输入 {@code LineString} 的平滑版本。
     *
     * @param ls    输入的 {@code LineString}
     * @param alpha 介于 0 到 1（含）之间的值，用于指定平滑后边界的贴合程度（0 为松散）
     * @return 平滑后的 {@code LineString}
     */
    LineString smooth(LineString ls, double alpha) {
        Coordinate[] coords = ls.getCoordinates();

        Coordinate[][] controlPoints = getLineControlPoints(coords, alpha);

        final int N = coords.length;
        List<Coordinate> smoothCoords = new ArrayList<>();
        double dist;
        for (int i = 0; i < N - 1; i++) {
            dist = coords[i].distance(coords[i + 1]);
            if (dist < control.getMinLength()) {
                // 线段过短 - 直接复制输入坐标
                smoothCoords.add(new Coordinate(coords[i]));

            } else {
                int smoothN = control.getNumVertices(dist);
                Coordinate[] segment = cubicBezier(
                        coords[i], coords[i + 1],
                        controlPoints[i][1], controlPoints[i + 1][0],
                        smoothN);

                int copyN = i < N - 1 ? segment.length - 1 : segment.length;
                smoothCoords.addAll(Arrays.asList(Arrays.copyOf(segment, copyN)));
            }
        }
        smoothCoords.add(coords[N - 1]);

        return geomFactory.createLineString(smoothCoords.toArray(new Coordinate[0]));
    }

    /**
     * 创建一个新的 {@code Polygon}，其外环是输入 {@code Polygon} 的平滑版本。
     * <p>
     * 注意：此方法目前会忽略空洞。
     *
     * @param p     输入的 {@code Polygon}
     * @param alpha 介于 0 到 1（含）之间的值，用于指定平滑后边界的贴合程度（0 为松散）
     * @return 平滑后的 {@code Polygon}
     */
    public Polygon smooth(Polygon p, double alpha) {
        Coordinate[] coords = p.getExteriorRing().getCoordinates();
        final int N = coords.length - 1;  // 首坐标 == 末坐标

        Coordinate[][] controlPoints = getPolygonControlPoints(coords, N, alpha);

        List<Coordinate> smoothCoords = new ArrayList<>();
        double dist;
        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;

            dist = coords[i].distance(coords[next]);
            if (dist < control.getMinLength()) {
                // 线段过短 - 直接复制输入坐标
                smoothCoords.add(new Coordinate(coords[i]));

            } else {
                int smoothN = control.getNumVertices(dist);
                Coordinate[] segment = cubicBezier(
                        coords[i], coords[next],
                        controlPoints[i][1], controlPoints[next][0],
                        smoothN);

                int copyN = i < N - 1 ? segment.length - 1 : segment.length;
                smoothCoords.addAll(Arrays.asList(Arrays.copyOf(segment, copyN)));
            }
        }

        LinearRing shell = geomFactory.createLinearRing(smoothCoords.toArray(new Coordinate[0]));
        return geomFactory.createPolygon(shell, null);
    }

    /**
     * 设置用于平滑的新 {@code Control} 对象。
     *
     * @param control 进行平滑时使用的控制对象；若为 {@code null}
     *                则使用默认控制
     */
    void setControl(SmootherControl control) {
        this.control = control == null ? defaultControl : control;
    }

    /**
     * 为 {@code Coordinates} 数组中的每个顶点计算一对 Bezier 控制点。
     *
     * @param coords 输入顶点
     * @param alpha  贴合程度
     * @return 二维 {@code Coordinates} 数组，表示每个输入顶点对应的一对控制点的位置
     */
    private Coordinate[][] getLineControlPoints(Coordinate[] coords, double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha must be a value between 0 and 1 inclusive");
        }

        final int N = coords.length;
        Coordinate[][] ctrl = new Coordinate[N][2];

        Coordinate[] v = new Coordinate[3];

        Coordinate[] mid = new Coordinate[2];
        mid[0] = new Coordinate();
        mid[1] = new Coordinate();

        Coordinate anchor = new Coordinate();
        double[] vdist = new double[2];

        // 先构造位于第一个真实坐标之前的虚拟坐标
        v[1] = new Coordinate(
                2 * coords[0].x - coords[1].x, 2 * coords[0].y - coords[1].y);
        v[2] = coords[0];

        // 用于线末端位置的虚拟坐标
        Coordinate vN = new Coordinate(
                2 * coords[N - 1].x - coords[N - 2].x,
                2 * coords[N - 1].y - coords[N - 2].y);

        mid[1].x = (v[1].x + v[2].x) / 2.0;
        mid[1].y = (v[1].y + v[2].y) / 2.0;
        vdist[1] = v[1].distance(v[2]);

        for (int i = 0; i < N; i++) {
            v[0] = v[1];
            v[1] = v[2];
            v[2] = (i < N - 1 ? coords[i + 1] : vN);

            mid[0].x = mid[1].x;
            mid[0].y = mid[1].y;
            mid[1].x = (v[1].x + v[2].x) / 2.0;
            mid[1].y = (v[1].y + v[2].y) / 2.0;

            vdist[0] = vdist[1];
            vdist[1] = v[1].distance(v[2]);

            double p = vdist[0] / (vdist[0] + vdist[1]);
            anchor.x = mid[0].x + p * (mid[1].x - mid[0].x);
            anchor.y = mid[0].y + p * (mid[1].y - mid[0].y);

            double xdelta = anchor.x - v[1].x;
            double ydelta = anchor.y - v[1].y;

            ctrl[i][0] = new Coordinate(
                    alpha * (v[1].x - mid[0].x + xdelta) + mid[0].x - xdelta,
                    alpha * (v[1].y - mid[0].y + ydelta) + mid[0].y - ydelta);

            ctrl[i][1] = new Coordinate(
                    alpha * (v[1].x - mid[1].x + xdelta) + mid[1].x - xdelta,
                    alpha * (v[1].y - mid[1].y + ydelta) + mid[1].y - ydelta);
        }

        return ctrl;
    }

    /**
     * 为 {@code Coordinates} 数组中的每个顶点计算一对 Bezier 控制点。
     *
     * @param coords 输入顶点
     * @param n      在 {@coords} 中使用的坐标数量
     * @param alpha  贴合程度
     * @return 二维 {@code Coordinates} 数组，表示每个输入顶点对应的一对控制点的位置
     */
    private Coordinate[][] getPolygonControlPoints(Coordinate[] coords, int n, double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha must be a value between 0 and 1 inclusive");
        }

        Coordinate[][] ctrl = new Coordinate[n][2];

        Coordinate[] v = new Coordinate[3];

        Coordinate[] mid = new Coordinate[2];
        mid[0] = new Coordinate();
        mid[1] = new Coordinate();

        Coordinate anchor = new Coordinate();
        double[] vdist = new double[2];

        v[1] = coords[n - 1];
        v[2] = coords[0];
        mid[1].x = (v[1].x + v[2].x) / 2.0;
        mid[1].y = (v[1].y + v[2].y) / 2.0;
        vdist[1] = v[1].distance(v[2]);

        for (int i = 0; i < n; i++) {
            v[0] = v[1];
            v[1] = v[2];
            v[2] = coords[(i + 1) % n];

            mid[0].x = mid[1].x;
            mid[0].y = mid[1].y;
            mid[1].x = (v[1].x + v[2].x) / 2.0;
            mid[1].y = (v[1].y + v[2].y) / 2.0;

            vdist[0] = vdist[1];
            vdist[1] = v[1].distance(v[2]);

            double p = vdist[0] / (vdist[0] + vdist[1]);
            anchor.x = mid[0].x + p * (mid[1].x - mid[0].x);
            anchor.y = mid[0].y + p * (mid[1].y - mid[0].y);

            double xdelta = anchor.x - v[1].x;
            double ydelta = anchor.y - v[1].y;

            ctrl[i][0] = new Coordinate(
                    alpha * (v[1].x - mid[0].x + xdelta) + mid[0].x - xdelta,
                    alpha * (v[1].y - mid[0].y + ydelta) + mid[0].y - ydelta);

            ctrl[i][1] = new Coordinate(
                    alpha * (v[1].x - mid[1].x + xdelta) + mid[1].x - xdelta,
                    alpha * (v[1].y - mid[1].y + ydelta) + mid[1].y - ydelta);
        }

        return ctrl;
    }

    /**
     * 给定起点、终点和两个控制点，计算三次 Bezier 曲线上的顶点。
     *
     * @param start 起点位置
     * @param end   终点位置
     * @param ctrl1 第一个控制点
     * @param ctrl2 第二个控制点
     * @param nv    顶点数量（包含起点和终点）
     * @return Bezier 曲线上的顶点
     */
    private Coordinate[] cubicBezier(final Coordinate start, final Coordinate end,
                                     final Coordinate ctrl1, final Coordinate ctrl2, final int nv) {

        final Coordinate[] curve = new Coordinate[nv];

        final Coordinate[] buf = new Coordinate[3];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = new Coordinate();
        }

        curve[0] = new Coordinate(start);
        curve[nv - 1] = new Coordinate(end);
        InterpPoint[] ip = getInterpPoints(nv);

        for (int i = 1; i < nv - 1; i++) {
            Coordinate c = new Coordinate();

            c.x = ip[i].t[0] * start.x + ip[i].t[1] * ctrl1.x + ip[i].t[2] * ctrl2.x + ip[i].t[3] * end.x;
            c.x /= ip[i].tsum;
            c.y = ip[i].t[0] * start.y + ip[i].t[1] * ctrl1.y + ip[i].t[2] * ctrl2.y + ip[i].t[3] * end.y;
            c.y /= ip[i].tsum;

            curve[i] = c;
        }

        return curve;
    }

    /**
     * 获取由给定顶点数量近似的 Bezier 曲线的插值参数。
     *
     * @param npoints 顶点数量
     * @return 保存参数值的 {@code InterpPoint} 对象数组
     */
    private InterpPoint[] getInterpPoints(int npoints) {
        WeakReference<InterpPoint[]> ref = lookup.get(npoints);
        InterpPoint[] ip = null;
        if (ref != null) ip = ref.get();

        if (ip == null) {
            ip = new InterpPoint[npoints];

            for (int i = 0; i < npoints; i++) {
                double t = (double) i / (npoints - 1);
                double tc = 1.0 - t;

                ip[i] = new InterpPoint();
                ip[i].t[0] = tc * tc * tc;
                ip[i].t[1] = 3.0 * tc * tc * t;
                ip[i].t[2] = 3.0 * tc * t * t;
                ip[i].t[3] = t * t * t;
                ip[i].tsum = ip[i].t[0] + ip[i].t[1] + ip[i].t[2] + ip[i].t[3];
            }

            lookup.put(npoints, new WeakReference<>(ip));
        }

        return ip;
    }

}
