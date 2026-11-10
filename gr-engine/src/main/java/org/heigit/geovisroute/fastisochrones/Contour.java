package org.heigit.geovisroute.fastisochrones;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.locationtech.jts.geom.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getMaxCellNodesNumber;
import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.isSupercellsEnabled;
import static org.locationtech.jts.algorithm.hull.ConcaveHull.concaveHullByLength;

/**
 * 计算单元的轮廓（Contour）。
 * 轮廓是给定点集的凹包。
 * 此外，还可以为超级单元创建轮廓。
 * 超级单元又被分组到另一组超超级单元中。
 * 超级单元最多包含由层级参数决定数量的基础单元。
 * 通常，超级单元中的基础单元会更少，因为部分分区分支会比其他分支更早结束。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class Contour {
    //多边形边必须以米为单位的长度，以便被拆分为更小的子边，从而避免后续凹包计算产生伪影
    private static final int MIN_EDGE_LENGTH = 125;
    private static final int MAX_EDGE_LENGTH = Integer.MAX_VALUE;
    //这意味着一个超级单元最多可包含 2^3 = 8 个基础单元。
    private static final int SUPER_CELL_HIERARCHY_LEVEL = 2;
    //这意味着一个超超级单元最多可包含 2^(3 + 2) = 32 个基础单元。
    private static final int SUPER_SUPER_CELL_HIERARCHY_LEVEL = 2; // 位于超级单元层级之上的层级
    private static final double CONCAVE_HULL_THRESHOLD = 0.006;
    private static final double BUFFER_SIZE = 0.0003;
    protected NodeAccess nodeAccess;
    protected BaseGraph baseGraph;
    protected OSMParsers osmParsers;
    private final IsochroneNodeStorage isochroneNodeStorage;
    private final CellStorage cellStorage;

    public Contour(BaseGraph baseGraph,OSMParsers osmParsers, NodeAccess nodeAccess, IsochroneNodeStorage isochroneNodeStorage, CellStorage cellStorage) {
        this.baseGraph = baseGraph;
        this.osmParsers = osmParsers;
        this.nodeAccess = nodeAccess;
        this.isochroneNodeStorage = isochroneNodeStorage;
        this.cellStorage = cellStorage;
    }

    /*
    计算两个坐标之间的距离（单位：米）
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2) {

        final int R = 6371; // 地球半径

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // 转换为米

        distance = Math.pow(distance, 2);

        return Math.sqrt(distance);
    }

    /**
     * 计算基础单元以及（若启用）超级单元的轮廓，并将数据存储在 cellStorage 中
     */
    public void calculateContour() {
        handleBaseCells();
        cellStorage.flush();
        IntObjectMap<IntHashSet> superCells = handleSuperCells();
        cellStorage.storeContourPointerMap();
        if (isSupercellsEnabled())
            cellStorage.storeSuperCells(superCells);
        cellStorage.setContourPrepared(true);
        cellStorage.flush();
    }

    /**
     * 为每个基础单元创建轮廓并存储
     */
    private void handleBaseCells() {
        for (IntCursor cellId : isochroneNodeStorage.getCellIds()) {
            List<Coordinate> coordinates = createCoordinates(cellId.value);
            LineString ring = createContour(coordinates);
            if (ring == null || ring.getNumPoints() < 2) {
                cellStorage.setCellContourOrder(cellId.value, new ArrayList<>(), new ArrayList<>());
                continue;
            }
            expandAndSaveContour(cellId.value, ring);
        }
    }

    /**
     * 为每个超级单元创建轮廓并存储
     * 创建超级单元是为了提升查询性能
     * 当前实现支持 2 级超级单元，并分别计算
     * 对于每个超级（超级）单元，我们需要知道对应的基础单元（以便从存储中获取轮廓）
     * 以及对应的子单元（对于超超级单元而言这些就是超级单元）
     *
     * @return 超级单元 ID -> 子单元 ID 集合的映射
     */
    private IntObjectMap<IntHashSet> handleSuperCells() {
        IntObjectMap<IntHashSet> superCells = new IntObjectHashMap<>();
        if (isSupercellsEnabled()) {
            superCells = identifySuperCells(isochroneNodeStorage.getCellIds(), SUPER_CELL_HIERARCHY_LEVEL, true);
            IntObjectMap<IntHashSet> superSuperCells = identifySuperCells(new IntHashSet(superCells.keys()), SUPER_SUPER_CELL_HIERARCHY_LEVEL, false);

            IntObjectMap<IntHashSet> superCellsToBaseCells = getBaseCellsOfSuperSuperCells(superSuperCells, superCells);
            superCellsToBaseCells.putAll(superCells);
            superCells.putAll(superSuperCells);

            //计算所有超级单元和超超级单元的凹包
            for (IntObjectCursor<IntHashSet> superCell : superCellsToBaseCells) {
                List<Coordinate> superCellCoordinates = createSuperCellCoordinates(superCell.value);
                LineString ring = createContour(superCellCoordinates);
                if (ring == null || ring.getNumPoints() < 2) {
                    cellStorage.setCellContourOrder(superCell.key, new ArrayList<>(), new ArrayList<>());
                    continue;
                }
                expandAndSaveContour(superCell.key, ring);
            }
        }
        return superCells;
    }

    /**
     * 从超级单元映射中获取每个超超级单元的所有基础单元
     *
     * @param superSuperCells 要为其获取基础单元的超超级单元
     * @param superCells      超级单元到基础单元的映射
     * @return 超超级单元到基础单元的映射
     */
    private IntObjectMap<IntHashSet> getBaseCellsOfSuperSuperCells(IntObjectMap<IntHashSet> superSuperCells, IntObjectMap<IntHashSet> superCells) {
        IntObjectMap<IntHashSet> baseCellsOfSuperSuperCells = new IntObjectHashMap<>();
        for (IntObjectCursor<IntHashSet> superSuperCell : superSuperCells) {
            IntHashSet newSuperCell = new IntHashSet();
            for (IntCursor cell : superSuperCell.value)
                newSuperCell.addAll(superCells.get(cell.value));
            baseCellsOfSuperSuperCells.put(superSuperCell.key, newSuperCell);
        }
        return baseCellsOfSuperSuperCells;
    }

    /**
     * 对于超级单元：从存储中获取基础单元的坐标。
     * 创建有序的坐标列表
     *
     * @param superCell 要为其创建坐标的超级单元
     * @return 基础单元的轮廓坐标列表
     */
    private List<Coordinate> createSuperCellCoordinates(IntHashSet superCell) {
        List<Coordinate> superCellCoordinates = new ArrayList<>(superCell.size() * 10);
        for (IntCursor subcell : superCell) {
            List<Double> subCellContour = cellStorage.getCellContourOrder(subcell.value);
            int j = 0;
            while (j < subCellContour.size()) {
                double lat = subCellContour.get(j);
                j++;
                double lon = subCellContour.get(j);
                j++;
                superCellCoordinates.add(new Coordinate(lon, lat));
            }
        }
        //需要对坐标排序，因为它们会被加入搜索树
        //插入顺序会改变搜索树的坐标，我们希望多次运行之间保持一致
        Collections.sort(superCellCoordinates);
        return superCellCoordinates;
    }

    private Geometry concHullOfNodes(List<Coordinate> points) {
        var geomFactory = new GeometryFactory();
        var geometries = points.stream().map(geomFactory::createPoint).toArray(Geometry[]::new);
        var treePoints = geomFactory.createGeometryCollection(geometries);

        return concaveHullByLength(treePoints, CONCAVE_HULL_THRESHOLD);
    }

    /**
     * 查找基础单元的超级单元。
     * 对于给定的 cellId，通过 cellId 位移查找与其相连的单元
     * 例如单元 100 和 101 是单元 10 的子单元
     *
     * @param cellIds        要查找其超级单元的 cellIds
     * @param hierarchyLevel 为找到超级单元应从 id 中裁剪多少位。会影响超级单元的大小
     * @param isPrimary      是为基础单元还是为已有超级单元查找超级单元
     * @return 超级单元 id 到基础单元 id 的映射
     */
    private IntObjectMap<IntHashSet> identifySuperCells(IntSet cellIds, int hierarchyLevel, boolean isPrimary) {
        //考虑 InertialFlow 最后一步中的子单元划分
        int maxId = createMaxId(cellIds);

        IntHashSet visitedCells = new IntHashSet();
        IntObjectMap<IntHashSet> superCells = new IntObjectHashMap<>();
        //对 id 排序可得到更好的超级单元，因为相近的 id 在地理上也更接近
        List<Integer> orderedCellIds = Arrays.stream(cellIds.toArray()).boxed().sorted().collect(Collectors.toList());
        for (int cellId : orderedCellIds) {
            if (visitedCells.contains(cellId))
                continue;
            //这些检查仅对由基础单元构建的超级单元需要且可行，对由超级单元构建的则不适用
            if (isPrimary && !isValidBaseCell(cellIds, cellId))
                continue;

            int motherId = cellId >> hierarchyLevel;
            //此单元在层级中位置过高，因此需要调整层级和 id
            while (motherId == 0) {
                hierarchyLevel -= 1;
                motherId = cellId >> hierarchyLevel;
            }

            IntHashSet superCell = new IntHashSet();

            createSuperCell(cellIds, visitedCells, superCell, maxId, motherId, isPrimary);
            for (IntCursor cell : superCell)
                visitedCells.add(cell.value);
            if (superCell.size() > 0)
                superCells.put(motherId, superCell);
        }
        return superCells;
    }

    private int createMaxId(IntSet cellIds) {
        int maxId = -1;
        for (IntCursor cellId : cellIds)
            if (cellId.value > maxId)
                maxId = cellId.value;
        return maxId;
    }

    /**
     * 检查基础单元是否有效。如果没有任何单元从它分离出来，且它本身也不是被分离的单元，则有效
     *
     * @param cellIds 所有已存在的 cellId
     * @param cellId  要检查的 cellId
     * @return isValid
     */
    private boolean isValidBaseCell(IntSet cellIds, int cellId) {
        //检查它是否属于被分离的单元：是否有子单元？
        if (cellIds.contains(cellId << 1))
            return false;
        return !isDisconnectedCell(cellIds, cellId);
    }

    private boolean isDisconnectedCell(IntSet cellIds, int cellId) {
        //如果它有姐妹单元，检查它们合并后的大小是否小于最小单元大小 -> 被分离
        return (cellIds.contains(cellId ^ 1) && cellStorage.getNodesOfCell(cellId).size()
                + cellStorage.getNodesOfCell(cellId ^ 1).size()
                < getMaxCellNodesNumber());
    }

    /**
     * 递归遍历 cellIds，收集属于某个超级单元 id 的所有单元
     *
     * @param cellIds      所有 cellId
     * @param visitedCells 已加入某个超级单元的 cellId
     * @param superCell    要向其添加其他 cell id 的超级单元
     * @param maxId        可能的最大搜索 id
     * @param currentCell  当前检查的单元
     * @param isPrimary    若超级单元由基础单元构建则为 true
     */
    private void createSuperCell(IntSet cellIds, IntHashSet visitedCells, IntHashSet superCell, int maxId, int currentCell, boolean isPrimary) {
        if (currentCell > maxId)
            return;
        //单元只能属于一个超级单元
        if (visitedCells.contains(currentCell))
            return;
        if (isPrimary && cellIds.contains(currentCell) && !isValidBaseCell(cellIds, currentCell))
            return;

        if (!cellIds.contains(currentCell)) {
            createSuperCell(cellIds, visitedCells, superCell, maxId, currentCell << 1, isPrimary);
            createSuperCell(cellIds, visitedCells, superCell, maxId, currentCell << 1 | 1, isPrimary);
        } else {
            superCell.add(currentCell);
        }
    }

    /**
     * 获取单元中所有节点和边的坐标
     *
     * @param cellId 单元 id
     * @return 表示单元中所有边和节点的坐标列表
     */
    private List<Coordinate> createCoordinates(int cellId) {
        IntHashSet cellNodes = cellStorage.getNodesOfCell(cellId);
        int initialSize = cellNodes.size();
        List<Coordinate> coordinates = new ArrayList<>(initialSize);
        AbstractAccessParser accessParser = (AbstractAccessParser) osmParsers.getWayTagParsers().get(0);
        EdgeFilter edgeFilter = AccessFilter.allEdges(accessParser.getAccessEnc()); // TODO 重构：清理方法链

        EdgeExplorer explorer = baseGraph.createEdgeExplorer(edgeFilter);
        EdgeIterator iter;

        IntHashSet visitedEdges = new IntHashSet();
        PointList towerCoordinates = new PointList(cellNodes.keys.length, false);
        for (IntCursor node : cellNodes) {
            towerCoordinates.add(baseGraph.getNodeAccess().getLat(node.value), baseGraph.getNodeAccess().getLon(node.value));
        }
        addLatLon(towerCoordinates, coordinates);
        for (IntCursor node : cellNodes) {
            iter = explorer.setBaseNode(node.value);
            while (iter.next()) {
                if (visitedEdges.contains(iter.getEdge())
                        || !cellNodes.contains(iter.getAdjNode())
                        || !edgeFilter.accept(iter))
                    continue;
                visitedEdges.add(iter.getEdge());
                splitAndAddLatLon(iter.fetchWayGeometry(FetchMode.ALL), coordinates, MIN_EDGE_LENGTH, MAX_EDGE_LENGTH);
            }
        }
        //去除重复项
        coordinates = coordinates.stream()
                .distinct()
                .collect(Collectors.toList());
        //需要对坐标排序，因为它们会被加入搜索树
        //插入顺序会改变搜索树的坐标，我们希望多次运行之间保持一致
        try {
            Collections.sort(coordinates);
        } catch (Exception e) {
            //这种情况在不到 1% 的运行中会出现，我还没有弄清楚原因。
            //它没有实际影响，因为排序只是为了轮廓的一致性，而非质量
        }
        return coordinates;
    }

    private LineString createContour(List<Coordinate> coordinates) {
        try {
            Geometry geom = concHullOfNodes(coordinates);
            Polygon poly = (Polygon) geom;
            poly.normalize();
            return poly.getExteriorRing();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 填充表示轮廓的多边形的边，使得即使很长的直线段也以规则的点表示。
     * 如果不拆分这些长边，会导致边中出现"空洞"，在由多个轮廓构建整体等时圈时可能被误判
     *
     * @param cellId 轮廓所属的 cellId
     * @param ring   按顺序表示轮廓的 LineString
     */
    private void expandAndSaveContour(int cellId, LineString ring) {
        List<Double> hullLatitudes = new ArrayList<>(ring.getNumPoints());
        List<Double> hullLongitudes = new ArrayList<>(ring.getNumPoints());
        for (int i = 0; i < ring.getNumPoints(); i++) {
            // 将坐标加入存储，但通过拆分确保长边上有足够的点
            hullLatitudes.add(ring.getPointN(i).getY());
            hullLongitudes.add(ring.getPointN(i).getX());
        }
        cellStorage.setCellContourOrder(cellId, hullLatitudes, hullLongitudes);
    }

    public Contour setGhStorage(BaseGraph basegraph) {
        this.baseGraph = baseGraph;
        return this;
    }

    private void splitAndAddLatLon(PointList newCoordinates, List<Coordinate> existingCoordinates, double minlim, double maxlim) {
        for (int i = 0; i < newCoordinates.size() - 1; i++) {
            double lat0 = newCoordinates.getLat(i);
            double lon0 = newCoordinates.getLon(i);
            double lat1 = newCoordinates.getLat(i + 1);
            double lon1 = newCoordinates.getLon(i + 1);
            double dist = distance(lat0, lat1, lon0, lon1);
            double dx = (lon0 - lon1);
            double dy = (lat0 - lat1);
            double normLength = Math.sqrt((dx * dx) + (dy * dy));

            int n = (int) Math.ceil(dist / minlim);
            double scale = BUFFER_SIZE / normLength;

            double dx2 = -dy * scale;
            double dy2 = dx * scale;
            if (i != 0) {
                existingCoordinates.add(new Coordinate(lon0 + dx2, lat0 + dy2));
                existingCoordinates.add(new Coordinate(lon0 - dx2, lat0 - dy2));
            }

            if (dist > minlim && dist < maxlim) {
                for (int j = 1; j < n; j++) {
                    existingCoordinates.add(new Coordinate(lon0 + j * (lon1 - lon0) / n + dx2, lat0 + j * (lat1 - lat0) / n + dy2));
                    existingCoordinates.add(new Coordinate(lon0 + j * (lon1 - lon0) / n - dx2, lat0 + j * (lat1 - lat0) / n - dy2));
                }
            }
        }
    }

    private void addLatLon(PointList newCoordinates, List<Coordinate> existingCoordinates) {
        if (newCoordinates.isEmpty())
            return;
        for (int i = 0; i < newCoordinates.size() - 1; i++) {
            double lat0 = newCoordinates.getLat(i);
            double lon0 = newCoordinates.getLon(i);
            existingCoordinates.add(new Coordinate(lon0 + BUFFER_SIZE, lat0 + BUFFER_SIZE));
            existingCoordinates.add(new Coordinate(lon0 - BUFFER_SIZE, lat0 - BUFFER_SIZE));
        }
    }
}
