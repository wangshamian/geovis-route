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
 */
package org.heigit.geovisroute.jts;

import org.geotools.metadata.i18n.ErrorKeys;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JTS 几何工具方法，将 Geotools 的能力引入 JTS。
 * <p>
 * 提供基于 geotools 的服务，例如重投影。
 * <p>
 * 职责：
 * <ul>
 * <li>坐标变换</li>
 * <li>坐标序列编辑</li>
 * <li>面向特定用途的通用坐标序列实现</li>
 * </ul>
 *
 * @author Jody Garnett
 * @author Martin Desruisseaux
 * @author Simone Giannecchini, GeoSolutions.
 * @author Michael Bedward
 * @version $Id$
 * @source $URL$
 * @since 2.2
 */
public final class JTS {
    private static final GeometryFactory factory;

    static {
        factory = new GeometryFactory();
    }

    /**
     * 不允许实例化此类。
     */
    private JTS() {
    }

    /**
     * 确保参数不为 null。
     *
     * @param name   参数名。
     * @param object 用户传入的参数。
     * @throws IllegalArgumentException 当 {@code object} 为 null 时抛出。
     */
    private static void ensureNonNull(final String name, final Object object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException(MessageFormat.format(ErrorKeys.NULL_ARGUMENT_$1, name));
        }
    }

    /**
     * 将指定 JTS 坐标的各坐标值复制到指定数组中。目标数组可为任意长度，
     * 只会复制源坐标中相关的字段。若数组长度大于 3，则所有多余维度
     * 都会被设为 {@link Double#NaN NaN}。
     *
     * @param point     源坐标。
     * @param ordinates 目标数组。
     */
    public static void copy(final Coordinate point, final double[] ordinates) {
        ensureNonNull("point", point);
        ensureNonNull("ordinates", ordinates);

        switch (ordinates.length) {
            case 3:
                ordinates[2] = point.z; // 继续向下执行

            case 2:
                ordinates[1] = point.y; // 继续向下执行

            case 1:
                ordinates[0] = point.x; // 继续向下执行

            case 0:
                break;

            default:
                Arrays.fill(ordinates, 3, ordinates.length, Double.NaN); // 继续向下执行
        }
    }

    /**
     * 创建输入几何对象的平滑副本。仅对多边形和线状几何有意义，
     * Point 对象将原样返回。平滑算法会插入新的顶点，
     * 这些顶点使用 Bezier 样条定位。输入几何对象的所有顶点
     * 都会保留在输出几何对象中。
     * <p>
     * {@code fit} 参数控制平滑后的线与输入线段之间的贴合程度，
     * 值为 1 时最贴合，为 0 时最松散。超出此范围的值
     * 会被自动向上或向下调整。
     * <p>
     * 输入几何对象可以是简单类型（例如 LineString、Polygon）、多类型（例如
     * MultiLineString、MultiPolygon）或 GeometryCollection。返回对象
     * 的类型与输入相同。
     *
     * @param geom 输入几何对象
     * @param fit  贴合程度，从 0（松散）到 1（贴合）
     * @return 与 {@code geom} 同类型的新 Geometry 对象
     * @throws IllegalArgumentException 当 {@code geom} 为 {@code null} 时抛出
     */
    public static Geometry smooth(final Geometry geom, double fit) {
        return smooth(geom, fit, new GeometryFactory());
    }

    /**
     * 创建输入几何对象的平滑副本。仅对多边形和线状几何有意义，
     * Point 对象将原样返回。平滑算法会插入新的顶点，
     * 这些顶点使用 Bezier 样条定位。输入几何对象的所有顶点
     * 都会保留在输出几何对象中。
     * <p>
     * {@code fit} 参数控制平滑后的线与输入线段之间的贴合程度，
     * 值为 1 时最贴合，为 0 时最松散。超出此范围的值
     * 会被自动向上或向下调整。
     * <p>
     * 输入几何对象可以是简单类型（例如 LineString、Polygon）、多类型（例如
     * MultiLineString、MultiPolygon）或 GeometryCollection。返回对象
     * 的类型与输入相同。
     *
     * @param geom    输入几何对象
     * @param fit     贴合程度，从 0（松散）到 1（贴合）
     * @param factory 用于创建平滑对象的 GeometryFactory
     * @return 与 {@code geom} 同类型的新 Geometry 对象
     * @throws IllegalArgumentException 当 {@code geom} 或 {@code factory} 为 {@code null} 时抛出
     */
    public static Geometry smooth(final Geometry geom, double fit, final GeometryFactory factory) {
        ensureNonNull("geom", geom);
        ensureNonNull("factory", factory);
        // 必要时调整 fit
        fit = Math.max(0.0, Math.min(1.0, fit));
        return smooth(geom, fit, factory, new GeometrySmoother(factory));
    }

    private static Geometry smooth(final Geometry geom, final double fit,
                                   final GeometryFactory factory, GeometrySmoother smoother) {
        return switch (geom.getGeometryType().toUpperCase()) {
            case "POINT", "MULTIPOINT" ->
                // 对于点，直接返回输入几何对象
                    geom;
            case "LINESTRING" ->
                // 这里同时处理开放线和闭合线（LinearRing）
                    smoothLineString(factory, smoother, geom, fit);
            case "MULTILINESTRING" -> smoothMultiLineString(factory, smoother, geom, fit);
            case "POLYGON" -> smoother.smooth((Polygon) geom, fit);
            case "MULTIPOLYGON" -> smoothMultiPolygon(factory, smoother, geom, fit);
            case "GEOMETRYCOLLECTION" -> smoothGeometryCollection(factory, smoother, geom, fit);
            default -> throw new UnsupportedOperationException("No smoothing method available for "
                    + geom.getGeometryType());
        };
    }

    private static Geometry smoothLineString(GeometryFactory factory, GeometrySmoother smoother,
                                             Geometry geom, double fit) {
        if (geom instanceof LinearRing linearRing) {
            // 按多边形处理
            Polygon poly = factory.createPolygon(linearRing, null);
            Polygon smoothed = smoother.smooth(poly, fit);
            return smoothed.getExteriorRing();
        } else {
            return smoother.smooth((LineString) geom, fit);
        }
    }

    private static Geometry smoothMultiLineString(GeometryFactory factory,
                                                  GeometrySmoother smoother, Geometry geom, double fit) {
        final int N = geom.getNumGeometries();
        LineString[] smoothed = new LineString[N];
        for (int i = 0; i < N; i++) {
            smoothed[i] = (LineString) smoothLineString(factory, smoother, geom.getGeometryN(i),
                    fit);
        }
        return factory.createMultiLineString(smoothed);
    }

    private static Geometry smoothMultiPolygon(GeometryFactory factory, GeometrySmoother smoother,
                                               Geometry geom, double fit) {
        final int N = geom.getNumGeometries();
        Polygon[] smoothed = new Polygon[N];

        for (int i = 0; i < N; i++) {
            smoothed[i] = smoother.smooth((Polygon) geom.getGeometryN(i), fit);
        }
        return factory.createMultiPolygon(smoothed);
    }

    private static Geometry smoothGeometryCollection(GeometryFactory factory,
                                                     GeometrySmoother smoother, Geometry geom, double fit) {
        final int N = geom.getNumGeometries();
        Geometry[] smoothed = new Geometry[N];
        for (int i = 0; i < N; i++) {
            smoothed[i] = smooth(geom.getGeometryN(i), fit, factory, smoother);
        }
        return factory.createGeometryCollection(smoothed);
    }


    /**
     * 从给定的 linestring 中移除共线点。
     *
     * @param ls 需要简化的 {@link LineString}。
     * @return 移除共线点后的 {@link LineString} 新实例。
     */
    static LineString removeCollinearVertices(final LineString ls) {
        if (ls == null) {
            throw new NullPointerException("The provided linestring is null");
        }
        final int N = ls.getNumPoints();
        final boolean isLinearRing = ls instanceof LinearRing;
        List<Coordinate> retain = new ArrayList<>();
        retain.add(ls.getCoordinateN(0));
        int i0 = 0;
        int i1 = 1;
        int i2 = 2;
        Coordinate firstCoord = ls.getCoordinateN(i0);
        Coordinate midCoord;
        Coordinate lastCoord;
        while (i2 < N) {
            midCoord = ls.getCoordinateN(i1);
            lastCoord = ls.getCoordinateN(i2);

            final int orientation = Orientation.index(firstCoord, midCoord, lastCoord);
            // 共线性检测
            if (orientation != Orientation.COLLINEAR) {
                // 加入中间点并移动起点
                retain.add(midCoord);
                i0 = i1;
                firstCoord = ls.getCoordinateN(i0);
            }
            i1++;
            i2++;
        }
        retain.add(ls.getCoordinateN(N - 1));

        //
        // 返回值
        //
        final int size = retain.size();
        // 没有任何变化？
        if (size == N) {
            // 释放全部并返回原对象
            retain.clear();

            return ls;
        }
        return isLinearRing ? ls.getFactory()
                .createLinearRing(retain.toArray(new Coordinate[size])) : ls.getFactory()
                .createLineString(retain.toArray(new Coordinate[size]));
    }

    /**
     * 从给定的 {@link Polygon} 中移除共线顶点。
     *
     * @param polygon 需要移除共线顶点的 {@link Polygon} 实例。
     * @return 不含共线顶点的 {@link Polygon} 新实例。
     */
    static Polygon removeCollinearVertices(final Polygon polygon) {
        if (polygon == null) {
            throw new NullPointerException("The provided Polygon is null");
        }

        // 复用已有的 factory
        final GeometryFactory gf = polygon.getFactory();

        // 处理外环
        LineString exterior = polygon.getExteriorRing();
        LineString shell = removeCollinearVertices(exterior);
        if ((shell == null) || shell.isEmpty()) {
            return null;
        }

        // 处理空洞
        List<LineString> holes = new ArrayList<>();
        final int size = polygon.getNumInteriorRing();
        for (int i = 0; i < size; i++) {
            LineString hole = polygon.getInteriorRingN(i);
            hole = removeCollinearVertices(hole);
            if ((hole != null) && !hole.isEmpty()) {
                holes.add(hole);
            }
        }

        return gf.createPolygon((LinearRing) shell, holes.toArray(new LinearRing[0]));
    }

    /**
     * 从给定的 {@link Geometry} 中移除共线顶点。
     *
     * <p>
     * 目前该实现仅支持 {@link Polygon}、{@link LineString} 和 {@link MultiPolygon}，
     * 若几何类型不属于这些类型则抛出异常
     *
     * @param g 需要移除共线顶点的 {@link Geometry} 实例。
     * @return 不含共线顶点的 {@link Geometry} 新实例。
     */
    public static Geometry removeCollinearVertices(final Geometry g) {
        if (g == null) {
            throw new NullPointerException("The provided Geometry is null");
        }
        if (g instanceof LineString lineString) {
            return removeCollinearVertices(lineString);
        } else if (g instanceof Polygon polygon) {
            return removeCollinearVertices(polygon);
        } else if (g instanceof MultiPolygon mp) {
            Polygon[] parts = new Polygon[mp.getNumGeometries()];
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                Polygon part = (Polygon) mp.getGeometryN(i);
                part = removeCollinearVertices(part);
                parts[i] = part;
            }
            return g.getFactory().createMultiPolygon(parts);
        }
        throw new IllegalArgumentException(
                "This method can work on LineString, Polygon and Multipolygon: " + g.getClass());
    }

    /**
     * 当点数超过指定的 minPoints 时，从给定的 {@link Geometry} 中移除共线顶点。
     *
     * <p>
     * 目前该实现仅支持 {@link Polygon}、{@link LineString} 和 {@link MultiPolygon}，
     * 若几何类型不属于这些类型则抛出异常
     *
     * @param geometry  需要移除共线顶点的 {@link Geometry} 实例。
     * @param minPoints 当顶点数超过 minPoints 时才执行共线点移除。
     * @return 不含共线顶点的 {@link Geometry} 新实例。
     */
    public static Geometry removeCollinearVertices(final Geometry geometry, int minPoints) {
        ensureNonNull("geometry", geometry);

        if ((minPoints <= 0) || (geometry.getNumPoints() < minPoints)) {
            return geometry;
        }
        if (geometry instanceof LineString lineString) {
            return removeCollinearVertices(lineString);
        } else if (geometry instanceof Polygon polygon) {
            return removeCollinearVertices(polygon);
        } else if (geometry instanceof MultiPolygon mp) {
            Polygon[] parts = new Polygon[mp.getNumGeometries()];
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                Polygon part = (Polygon) mp.getGeometryN(i);
                part = removeCollinearVertices(part);
                parts[i] = part;
            }

            return geometry.getFactory().createMultiPolygon(parts);
        }
        throw new IllegalArgumentException(
                "This method can work on LineString, Polygon and Multipolygon: "
                        + geometry.getClass());
    }

    public static Polygon toGeometry(final Envelope env) {
        return toGeometry(env, factory);
    }

    public static Polygon toGeometry(final Envelope env, GeometryFactory factory) {
        ensureNonNull("env", env);
        return factory.createPolygon(factory.createLinearRing(new Coordinate[]{
                new Coordinate(env.getMinX(), env.getMinY()),
                new Coordinate(env.getMaxX(), env.getMinY()),
                new Coordinate(env.getMaxX(), env.getMaxY()),
                new Coordinate(env.getMinX(), env.getMaxY()),
                new Coordinate(env.getMinX(), env.getMinY())}), null);
    }
}
