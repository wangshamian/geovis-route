package com.graphhopper.offroad;

import com.graphhopper.GHRequest;
import com.graphhopper.OffRoadResponse;
import com.graphhopper.offroad.util.GeoBounds;
import com.graphhopper.offroad.util.GeoUtils;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

public class OffRoader {
    /**
     * 走廊最小/最大半宽（米）：搜索范围限制在起终点连线两侧一定范围内，避免整片矩形区域都参与计算。
     */
    private static final double MIN_CORRIDOR_METERS = 1500;
    private static final double MAX_CORRIDOR_METERS = 8000;
    /**
     * 走廊半宽与起终点直线距离的比例。
     */
    private static final double CORRIDOR_RATIO = 0.5;
    /**
     * 默认最大可通行坡度（度）。
     */
    private static final double DEFAULT_MAX_SLOPE = 20;
    /**
     * 默认最多网格数量与超时时间。
     */
    private static final int DEFAULT_MAX_CELLS = 250_000;
    private static final long DEFAULT_TIMEOUT_MS = 30_000;

    protected final ElevationProvider eleProvider;
    protected OffRoadGrid startGrid;
    protected OffRoadGrid endGrid;
    private double maxSlope = DEFAULT_MAX_SLOPE;
    private GHPoint startPoint;
    private GHPoint endPoint;
    /**
     * 八邻域偏移：{dx, dy}，前四个为正交方向，后四个为斜向方向。
     */
    private static final int[][] NEIGHBOR_OFFSETS = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0}, {-1, -1}, {1, -1}, {1, 1}, {-1, 1}
    };

    public OffRoader(ElevationProvider eleProvider) {
        this.eleProvider = eleProvider;
    }

    public OffRoadResponse offroad(GHRequest request) {
        OffRoadResponse offRoadResponse = new OffRoadResponse();
        double gridInterval = request.getHints().getDouble("grid_interval", 300);
        // 坡度阈值、网格数量上限与超时时间均可通过 hint 配置，便于前端按需调整
        this.maxSlope = request.getHints().getDouble("max_slope", DEFAULT_MAX_SLOPE);
        int maxCells = request.getHints().getInt("max_cells", DEFAULT_MAX_CELLS);
        long timeoutMs = request.getHints().getLong("timeout_ms", DEFAULT_TIMEOUT_MS);
        long deadline = System.currentTimeMillis() + timeoutMs;

        List<GHPoint> points = request.getPoints();
        this.startPoint = points.get(0);
        this.endPoint = points.get(points.size() - 1);

        // 起终点顺序任意，构造边界时统一按最小值/最大值归一化，避免出现负的跨度
        GeoBounds geoBounds = new GeoBounds(
                Math.min(startPoint.lon, endPoint.lon), Math.max(startPoint.lon, endPoint.lon),
                Math.min(startPoint.lat, endPoint.lat), Math.max(startPoint.lat, endPoint.lat));

        // 走廊式搜索：以起终点直线距离的一半作为搜索半宽，并限制在固定上下界内，
        // 相比按比例放大整片矩形，可显著减少参与计算的网格数量。
        double directDistance = GeoUtils.getDistance(
                new GlobalCoordinates(startPoint.lat, startPoint.lon),
                new GlobalCoordinates(endPoint.lat, endPoint.lon));
        double halfWidth = Math.min(MAX_CORRIDOR_METERS, Math.max(MIN_CORRIDOR_METERS, directDistance * CORRIDOR_RATIO));
        // 保证走廊至少覆盖几个网格，避免极短路径时搜索空间过窄
        halfWidth = Math.max(halfWidth, gridInterval * 3);
        GeoBounds corridor = geoBounds.expandBoundsByMeters(halfWidth);

        List<OffRoadGrid> gridCells = createOffRoadGrids(corridor, gridInterval, this.eleProvider, startPoint,
                endPoint, maxCells, deadline, timeoutMs);
        if (gridCells.isEmpty()) {
            return offRoadResponse;
        }
        return calOffRoadPath(offRoadResponse, gridCells, gridInterval, deadline, timeoutMs);
    }

    /**
     * 获取grid
     */
    public List<OffRoadGrid> createOffRoadGrids(GeoBounds geoBounds, double cellSide, ElevationProvider eleProvider,
                                                GHPoint startPoint, GHPoint endPoint, int maxCells, long deadline,
                                                long timeoutMs) {
        int[][] offRoadGridSteps = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {-1, 3}, {-2, 3}, {-3, 3},
                {-3, 2}, {-3, 1}, {-3, 0}, {-2, 0}, {-1, 0}};
        double lonDistance = GeoUtils.getDistance(new GlobalCoordinates(geoBounds.maxLat, geoBounds.minLon),
                new GlobalCoordinates(geoBounds.maxLat, geoBounds.maxLon));
        double latDistance = GeoUtils.getDistance(new GlobalCoordinates(geoBounds.maxLat, geoBounds.minLon),
                new GlobalCoordinates(geoBounds.minLat, geoBounds.minLon));
        // rows 为纬度方向（y）的网格行数，cols 为经度方向（x）的网格列数
        int rows = Math.max(1, (int) Math.ceil(latDistance / cellSide));
        int cols = Math.max(1, (int) Math.ceil(lonDistance / cellSide));

        // 网格数量保护：超过上限直接拒绝，避免内存与耗时失控
        long totalCells = (long) rows * cols;
        if (totalCells > maxCells) {
            throw new IllegalArgumentException(String.format(
                    "网格数量 %d 超过上限 %d，请增大 grid_interval 或缩短起终点距离", totalCells, maxCells));
        }

        List<OffRoadGrid> grids = new ArrayList<>((int) totalCells);
        double cellWidth = geoBounds.getWidth() / cols;
        double cellHeight = geoBounds.getHeight() / rows;
        this.startGrid = null;
        this.endGrid = null;

        // 开始计算OffRoadGrid
        int processed = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 每处理一小批网格检查一次超时，避免高程采样阶段无限占用线程
                if ((processed++ & 127) == 0 && System.currentTimeMillis() > deadline) {
                    throw new IllegalArgumentException("越野路径计算超时（超过 " + timeoutMs
                            + " 毫秒），请增大网格间距或缩短起终点距离");
                }
                double minX = geoBounds.minLon + j * cellWidth;
                double maxX = minX + cellWidth;
                double minY = geoBounds.minLat + i * cellHeight;
                double maxY = minY + cellHeight;

                // 获取Grid的属性值
                GHPoint center = new GHPoint((minY + maxY) / 2, (minX + maxX) / 2);
                List<GHPoint> coordinates = new ArrayList<>();
                coordinates.add(new GHPoint(minY, minX));
                coordinates.add(new GHPoint(minY, maxX));
                coordinates.add(new GHPoint(maxY, maxX));
                coordinates.add(new GHPoint(maxY, minX));
                coordinates.add(new GHPoint(minY, minX));
                // 采样并计算坡度
                List<GlobalCoordinates> sampleCoordinates = new ArrayList<>();
                double sampleDeltaX = cellWidth / 3;
                double sampleDeltaY = cellHeight / 3;
                for (int k = 0; k < 12; k++) {
                    sampleCoordinates.add(new GlobalCoordinates(minY + offRoadGridSteps[k][0] * sampleDeltaY,
                            minX + offRoadGridSteps[k][1] * sampleDeltaX));
                }
                double slope = GeoUtils.getGridSlope(sampleCoordinates, eleProvider);
                OffRoadGrid offRoadGrid = new OffRoadGrid(j, i, slope, center, coordinates);
                grids.add(offRoadGrid);

                // 直接复用网格对象作为起终点，确保其中心点与坡度信息正确
                if (this.startGrid == null && minX <= startPoint.lon && startPoint.lon <= maxX
                        && minY <= startPoint.lat && startPoint.lat <= maxY) {
                    this.startGrid = offRoadGrid;
                }
                if (this.endGrid == null && minX <= endPoint.lon && endPoint.lon <= maxX
                        && minY <= endPoint.lat && endPoint.lat <= maxY) {
                    this.endGrid = offRoadGrid;
                }
            }
        }

        return grids;
    }

    private OffRoadResponse calOffRoadPath(OffRoadResponse offRoadResponse, List<OffRoadGrid> grids,
                                           double cellSide, long deadline, long timeoutMs) {
        PointList roadPath = new PointList();
        JsonFeature barrierFeature = new JsonFeature();
        GeometryFactory geometryFactory = new GeometryFactory();
        List<Polygon> barrierPolygons = new ArrayList<>();

        if (grids.isEmpty()) {
            offRoadResponse.addOffRoadPoints(roadPath);
            return offRoadResponse;
        }

        // 建立二维索引，邻居查找降为 O(1)
        int rows = 0;
        int cols = 0;
        for (OffRoadGrid grid : grids) {
            rows = Math.max(rows, grid.y + 1);
            cols = Math.max(cols, grid.x + 1);
        }
        OffRoadGrid[][] cellIndex = new OffRoadGrid[rows][cols];
        for (OffRoadGrid grid : grids) {
            cellIndex[grid.y][grid.x] = grid;
            // 起终点所在网格始终放行，避免用户选点恰好落在陡坡上导致无法起算
            if (grid.slope > maxSlope && grid != startGrid && grid != endGrid) {
                // 坡度超限的网格视为障碍，直接关闭
                grid.isInCloseList = true;
                Coordinate[] coordinates = new Coordinate[grid.coordinates.size()];
                for (int i = 0; i < grid.coordinates.size(); i++) {
                    GHPoint gridPoint = grid.coordinates.get(i);
                    coordinates[i] = new Coordinate(gridPoint.lon, gridPoint.lat);
                }
                barrierPolygons.add(geometryFactory.createPolygon(coordinates));
            } else {
                // 可通行网格初始化代价
                grid.g = Double.MAX_VALUE;
                grid.h = 0;
                grid.f = Double.MAX_VALUE;
                grid.isInOpenList = false;
                grid.isInCloseList = false;
                grid.parentId = null;
            }
        }
        // 相邻障碍网格合并为面，减少返回的多边形数量与前端绘制压力
        offRoadResponse.setBarrierCount(barrierPolygons.size());
        barrierFeature.setGeometry(mergeBarrierPolygons(geometryFactory, barrierPolygons));
        offRoadResponse.addBarrierFeature(barrierFeature);

        if (startGrid == null || endGrid == null) {
            // 起终点不在搜索范围内，返回空路径
            offRoadResponse.addOffRoadPoints(roadPath);
            return offRoadResponse;
        }

        // 标准 A*：按 f = g + h 升序扩展代价最小的节点
        PriorityQueue<OffRoadGrid> openList = new PriorityQueue<>(Comparator.comparingDouble(grid -> grid.f));
        startGrid.g = 0;
        startGrid.parentId = null;
        startGrid.h = heuristic(startGrid, endGrid);
        startGrid.f = startGrid.h;
        startGrid.isInOpenList = true;
        openList.add(startGrid);

        OffRoadGrid endNode = null;
        int expanded = 0;
        while (!openList.isEmpty()) {
            if ((expanded++ & 4095) == 0 && System.currentTimeMillis() > deadline) {
                throw new IllegalArgumentException("越野路径搜索超时（超过 " + timeoutMs
                        + " 毫秒），请增大网格间距或缩短起终点距离");
            }
            OffRoadGrid nodeCurrent = openList.poll();
            // 同一节点可能因代价更新被重复入队，已关闭的旧条目直接跳过
            if (nodeCurrent.isInCloseList) {
                continue;
            }
            nodeCurrent.isInCloseList = true;
            nodeCurrent.isInOpenList = false;
            if (nodeCurrent == endGrid) {
                endNode = nodeCurrent;
                break;
            }

            // 遍历八邻域
            for (int[] offset : NEIGHBOR_OFFSETS) {
                OffRoadGrid child = getNeighbor(cellIndex, nodeCurrent, offset[0], offset[1]);
                if (child == null || child.isInCloseList) {
                    continue;
                }
                boolean diagonal = offset[0] != 0 && offset[1] != 0;
                // 斜向移动时，两侧正交网格不能同时是障碍，避免从障碍拐角穿过
                if (diagonal) {
                    OffRoadGrid horizontal = getNeighbor(cellIndex, nodeCurrent, offset[0], 0);
                    OffRoadGrid vertical = getNeighbor(cellIndex, nodeCurrent, 0, offset[1]);
                    if (isBlocked(horizontal) && isBlocked(vertical)) {
                        continue;
                    }
                }
                double stepCost = diagonal ? Math.sqrt(2) : 1;
                double tentativeG = nodeCurrent.g + stepCost;
                // 找到更优路径才更新父节点与代价
                if (tentativeG < child.g) {
                    child.g = tentativeG;
                    child.h = heuristic(child, endGrid);
                    child.f = child.g + child.h;
                    child.parentId = nodeCurrent.id;
                    if (!child.isInOpenList) {
                        child.isInOpenList = true;
                    }
                    openList.add(child);
                }
            }
        }

        if (endNode == null) {
            // 起终点之间不存在可通行路径
            offRoadResponse.addOffRoadPoints(roadPath);
            return offRoadResponse;
        }

        // 回溯父节点，得到起点到终点的网格路径
        List<OffRoadGrid> pathGrids = new ArrayList<>();
        OffRoadGrid node = endNode;
        while (node != null) {
            pathGrids.add(node);
            node = parentOf(cellIndex, node);
        }
        Collections.reverse(pathGrids);

        // 视线拉直 + 抽稀 + 平滑，消除格心连线的锯齿与绕路
        List<GHPoint> path = smoothPath(pathGrids, cellIndex, cellSide);
        for (GHPoint point : path) {
            roadPath.add(point);
        }
        offRoadResponse.addOffRoadPoints(roadPath);
        return offRoadResponse;
    }

    /**
     * 路径后处理：先用视线检测跳过可直连的中间格心，再用 Douglas-Peucker 抽稀，最后用 Chaikin 做一次平滑。
     */
    private List<GHPoint> smoothPath(List<OffRoadGrid> pathGrids, OffRoadGrid[][] cellIndex, double cellSide) {
        if (pathGrids.size() == 1) {
            // 起终点落在同一网格，直接连接两个真实坐标
            List<GHPoint> direct = new ArrayList<>();
            direct.add(startPoint);
            direct.add(endPoint);
            return direct;
        }

        List<GHPoint> points = new ArrayList<>();
        for (OffRoadGrid grid : lineOfSightSimplify(pathGrids, cellIndex)) {
            points.add(grid.center);
        }
        // 首尾替换为真实起终点，避免路径端点偏移到格心
        points.set(0, startPoint);
        points.set(points.size() - 1, endPoint);

        List<GHPoint> simplified = douglasPeucker(points, Math.max(cellSide * 0.5, 1));
        return chaikinOnce(simplified);
    }

    /**
     * 视线拉直：从当前锚点出发，尽量连接到最远的、与锚点之间无障碍的路径网格。
     */
    private List<OffRoadGrid> lineOfSightSimplify(List<OffRoadGrid> path, OffRoadGrid[][] cellIndex) {
        if (path.size() <= 2) {
            return path;
        }
        List<OffRoadGrid> result = new ArrayList<>();
        result.add(path.get(0));
        int anchor = 0;
        while (anchor < path.size() - 1) {
            int next = anchor + 1;
            for (int j = path.size() - 1; j > anchor + 1; j--) {
                if (hasLineOfSight(path.get(anchor), path.get(j), cellIndex)) {
                    next = j;
                    break;
                }
            }
            result.add(path.get(next));
            anchor = next;
        }
        return result;
    }

    /**
     * 判断两个网格之间的连线是否全部落在可通行网格内（每格采样 4 次）。
     */
    private boolean hasLineOfSight(OffRoadGrid from, OffRoadGrid to, OffRoadGrid[][] cellIndex) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) * 4);
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(from.x + dx * t);
            int y = (int) Math.round(from.y + dy * t);
            if (y < 0 || y >= cellIndex.length || x < 0 || x >= cellIndex[0].length
                    || isBlocked(cellIndex[y][x])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Douglas-Peucker 抽稀，tolerance 为垂直距离阈值（米）。
     */
    private List<GHPoint> douglasPeucker(List<GHPoint> points, double tolerance) {
        int n = points.size();
        if (n < 3) {
            return points;
        }
        // 以平均纬度做等距投影，把经纬度换算成米，便于按米比较距离
        double meanLat = 0;
        for (GHPoint point : points) {
            meanLat += point.lat;
        }
        meanLat /= n;
        double lonScale = 111320.0 * Math.cos(Math.toRadians(meanLat));
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = points.get(i).lon * lonScale;
            ys[i] = points.get(i).lat * 111320.0;
        }
        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, n - 1});
        while (!stack.isEmpty()) {
            int[] range = stack.pop();
            int first = range[0];
            int last = range[1];
            double maxDistance = -1;
            int index = -1;
            for (int i = first + 1; i < last; i++) {
                double distance = perpendicularDistance(xs[i], ys[i], xs[first], ys[first], xs[last], ys[last]);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    index = i;
                }
            }
            if (index != -1 && maxDistance > tolerance) {
                keep[index] = true;
                stack.push(new int[]{first, index});
                stack.push(new int[]{index, last});
            }
        }
        List<GHPoint> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                result.add(points.get(i));
            }
        }
        return result;
    }

    private double perpendicularDistance(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) {
            return Math.hypot(px - ax, py - ay);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    /**
     * Chaikin 平滑一次，让折线拐角变得圆滑，同时保持首尾点不变。
     */
    private List<GHPoint> chaikinOnce(List<GHPoint> points) {
        if (points.size() < 3) {
            return points;
        }
        List<GHPoint> result = new ArrayList<>(points.size() * 2);
        result.add(points.get(0));
        for (int i = 0; i < points.size() - 1; i++) {
            GHPoint a = points.get(i);
            GHPoint b = points.get(i + 1);
            result.add(new GHPoint(a.lat * 0.75 + b.lat * 0.25, a.lon * 0.75 + b.lon * 0.25));
            result.add(new GHPoint(a.lat * 0.25 + b.lat * 0.75, a.lon * 0.25 + b.lon * 0.75));
        }
        result.add(points.get(points.size() - 1));
        return result;
    }

    /**
     * 障碍网格合并：相邻网格取并集，减少返回与渲染的多边形数量。
     */
    private Geometry mergeBarrierPolygons(GeometryFactory geometryFactory, List<Polygon> polygons) {
        if (polygons.isEmpty()) {
            return geometryFactory.createMultiPolygon(new Polygon[0]);
        }
        if (polygons.size() == 1) {
            return polygons.get(0);
        }
        try {
            return UnaryUnionOp.union(polygons);
        } catch (RuntimeException e) {
            // 合并失败时退化为原始网格面，保证接口仍然可用
            return geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
        }
    }

    /**
     * 八方向移动的 octile 距离，作为一致的启发函数，保证 A* 结果最优。
     */
    private double heuristic(OffRoadGrid from, OffRoadGrid to) {
        int dx = Math.abs(from.x - to.x);
        int dy = Math.abs(from.y - to.y);
        return (dx + dy) + (Math.sqrt(2) - 2) * Math.min(dx, dy);
    }

    /**
     * 获取当前网格指定偏移处的邻居，越界返回 null。
     */
    private OffRoadGrid getNeighbor(OffRoadGrid[][] cellIndex, OffRoadGrid current, int dx, int dy) {
        int x = current.x + dx;
        int y = current.y + dy;
        if (y < 0 || y >= cellIndex.length || x < 0 || x >= cellIndex[0].length) {
            return null;
        }
        return cellIndex[y][x];
    }

    /**
     * 判断网格是否不可通行（越界或坡度超限）。
     */
    private boolean isBlocked(OffRoadGrid grid) {
        return grid == null || grid.slope > maxSlope;
    }

    /**
     * 根据 parentId（格式为 "x-y"）取回父网格。
     */
    private OffRoadGrid parentOf(OffRoadGrid[][] cellIndex, OffRoadGrid node) {
        if (node.parentId == null) {
            return null;
        }
        int separator = node.parentId.indexOf('-');
        int x = Integer.parseInt(node.parentId.substring(0, separator));
        int y = Integer.parseInt(node.parentId.substring(separator + 1));
        if (y < 0 || y >= cellIndex.length || x < 0 || x >= cellIndex[0].length) {
            return null;
        }
        return cellIndex[y][x];
    }
}
