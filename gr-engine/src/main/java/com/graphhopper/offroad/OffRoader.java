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
import org.locationtech.jts.geom.*;

import java.util.*;
import java.util.stream.Collectors;

public class OffRoader {
    protected final ElevationProvider eleProvider;
    protected OffRoadGrid startGrid;
    protected OffRoadGrid endGrid;
    private final int maxSlope = 20;

    public OffRoader(ElevationProvider eleProvider) {
        this.eleProvider = eleProvider;
    }

    public OffRoadResponse offroad(GHRequest request) {
        OffRoadResponse offRoadResponse = new OffRoadResponse();
        double gridInterval = request.getHints().getDouble("grid_interval", 10);
        List<GHPoint> points = request.getPoints();
        GHPoint startPoint = points.get(0);
        GHPoint endPoint = points.get(points.size() - 1);
        GeoBounds geoBounds = new GeoBounds(startPoint.lon, endPoint.lon, startPoint.lat, endPoint.lat);

        // 扩大矩形边界（度数）
        GeoBounds expanded = geoBounds.expandBoundsByRatio(5);

        // 按指定行列数分块
        List<OffRoadGrid> gridCells = createOffRoadGrids(expanded,gridInterval, this.eleProvider, startPoint,
                endPoint);
        offRoadResponse = calOffRoadPath(offRoadResponse, gridCells);
        return offRoadResponse;
    }

    /**
     * 获取grid
     */
    public List<OffRoadGrid> createOffRoadGrids(GeoBounds geoBounds, double cellSide, ElevationProvider eleProvider,
                                                GHPoint startPoint, GHPoint endPoint) {
        int[][] OffRoadGridSteps = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {-1, 3}, {-2, 3}, {-3, 3},
                {-3, 2}, {-3, 1}, {-3, 0}, {-2, 0}, {-1, 0}};
        double lonDistance = GeoUtils.getDistance(new GlobalCoordinates(geoBounds.maxLat, geoBounds.minLon),
                new GlobalCoordinates(geoBounds.maxLat, geoBounds.maxLon));
        double latDistance = GeoUtils.getDistance(new GlobalCoordinates(geoBounds.maxLat, geoBounds.minLon),
                new GlobalCoordinates(geoBounds.minLat, geoBounds.minLon));
        int rows = (int) Math.ceil(lonDistance / cellSide);
        int cols = (int) Math.ceil(latDistance / cellSide);
        List<OffRoadGrid> grids = new ArrayList<>();

        double cellWidth = geoBounds.getWidth() / cols;
        double cellHeight = geoBounds.getHeight() / rows;

        // 开始计算OffRoadGrid
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double minX = geoBounds.minLon + j * cellWidth;
                double maxX = minX + cellWidth;
                double minY = geoBounds.minLat + i * cellHeight;
                double maxY = minY + cellHeight;

                //判断开始和结束点是否在该方格内
                Envelope gridEnvelope = new Envelope(minX, maxX, minY, maxY);
                if (gridEnvelope.contains(startPoint.lon, startPoint.lat)) {
                    this.startGrid = new OffRoadGrid(j, i, 0, new GHPoint(), new ArrayList<>());
                }
                if (gridEnvelope.contains(endPoint.lon, endPoint.lat)) {
                    this.endGrid = new OffRoadGrid(j, i, 0, new GHPoint(), new ArrayList<>());
                }

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
                    sampleCoordinates.add(new GlobalCoordinates(minY + OffRoadGridSteps[k][0] * sampleDeltaY,
                            minX + OffRoadGridSteps[k][1] * sampleDeltaX));
                }
                double slope = GeoUtils.getGridSlope(sampleCoordinates, eleProvider);
                OffRoadGrid offRoadGrid = new OffRoadGrid(j, i, slope, center, coordinates);

                grids.add(offRoadGrid);
            }
        }

        return grids;
    }

    private OffRoadResponse calOffRoadPath(OffRoadResponse offRoadResponse, List<OffRoadGrid> grids) {
        PointList roadPath = new PointList();
        JsonFeature barrierFeature = new JsonFeature();
        GeometryFactory geometryFactory = new GeometryFactory();
        List<OffRoadGrid> allList = new ArrayList<>(); //所有网格
        List<OffRoadGrid> openList = new ArrayList<>(); //待计算的网格
        List<Polygon> multiCoordinates = new ArrayList<>();
        for (OffRoadGrid grid : grids) {
            grid.h = Math.sqrt(
                    Math.pow(grid.x - this.startGrid.x, 2) +
                            Math.pow(grid.y - this.endGrid.y, 2)
            );
            if (grid.slope > maxSlope) {
                grid.isInCloseList = true; //障碍物就关闭，后面不再对该网格进行计算
                Coordinate[] coordinates = new Coordinate[grid.coordinates.size()];
                for (int i = 0; i < grid.coordinates.size(); i++) {
                    GHPoint gridPoint = grid.coordinates.get(i);
                    coordinates[i] = new Coordinate(gridPoint.lon, gridPoint.lat);
                }
                multiCoordinates.add(geometryFactory.createPolygon(coordinates));
            } else {
                grid.isInOpenList = false; //该网格为待计算网格
            }
            allList.add(grid);
        }
        barrierFeature.setGeometry(geometryFactory.createMultiPolygon(multiCoordinates.stream().toArray(Polygon[]::new)));
        offRoadResponse.addBarrierFeature(barrierFeature);
        this.startGrid.g = 0;
        this.startGrid.isInOpenList = true;
        //计算好起点的代价后将它插入到openList中
        openList.add(this.startGrid);

        OffRoadGrid endNode = null;
        while (openList.size() > 0) {
            //根据代价逆序排序，从openList中获取到代价最小的网格（如果有多个代价相同的点，优先选择 g 值（历史代价）较小的网格，因为这更有可能导向最短路径。）
            Collections.sort(openList, new Comparator<OffRoadGrid>() {
                @Override
                public int compare(OffRoadGrid a, OffRoadGrid b) {
                    return Double.compare(b.f, a.f); // 降序
                }
            });

            Collections.sort(openList, new Comparator<OffRoadGrid>() {
                @Override
                public int compare(OffRoadGrid a, OffRoadGrid b) {
                    return Double.compare(b.f, a.f); // 降序
                }
            });
            List<OffRoadGrid> minFNodes = openList.stream().filter(item -> item.f == openList.get(0).f).collect(Collectors.toList());
            Collections.sort(minFNodes, new Comparator<OffRoadGrid>() {
                @Override
                public int compare(OffRoadGrid a, OffRoadGrid b) {
                    return Double.compare(b.g, a.g); // 降序
                }
            });
            OffRoadGrid nodeCurrent = minFNodes.get(0);
            //获取代价最小的网格周围的网格
            OffRoadGrid childUp = allList.stream().filter(
                    item -> item.x == nodeCurrent.x && item.y == nodeCurrent.y - 1
            ).findFirst().orElse(null);
            OffRoadGrid childRight = allList.stream().filter(
                    item -> item.x == nodeCurrent.x + 1 && item.y == nodeCurrent.y
            ).findFirst().orElse(null);
            OffRoadGrid childDown = allList.stream().filter(
                    item -> item.x == nodeCurrent.x && item.y == nodeCurrent.y + 1
            ).findFirst().orElse(null);
            OffRoadGrid childLeft = allList.stream().filter(
                    item -> item.x == nodeCurrent.x - 1 && item.y == nodeCurrent.y
            ).findFirst().orElse(null);
            List<OffRoadGrid> childList = new ArrayList<>();
            childList.add(childUp);
            childList.add(childRight);
            childList.add(childDown);
            childList.add(childLeft);
            //只有当左边和上边不全是障碍物，才能走左上的网格
            if ((childUp != null && !childUp.isInCloseList) || (childLeft != null && !childLeft.isInCloseList)) {
                OffRoadGrid childLeftUp = allList.stream().filter(
                        item -> item.x == nodeCurrent.x - 1 && item.y == nodeCurrent.y - 1
                ).findFirst().orElse(null);
                childList.add(childLeftUp);
            }
            //只有当右边和上边不全是障碍物，才能走右上的网格
            if ((childUp != null && !childUp.isInCloseList) || (childRight != null && !childRight.isInCloseList)) {
                OffRoadGrid childRightUp = allList.stream().filter(
                        item -> item.x == nodeCurrent.x + 1 && item.y == nodeCurrent.y - 1
                ).findFirst().orElse(null);
                childList.add(childRightUp);
            }
            //只有当右边和下边不全是障碍物，才能走右下的网格
            if ((childDown != null && !childDown.isInCloseList) || (childRight != null && !childRight.isInCloseList)) {
                OffRoadGrid childRightDown = allList.stream().filter(
                        item -> item.x == nodeCurrent.x + 1 && item.y == nodeCurrent.y + 1
                ).findFirst().orElse(null);
                childList.add(childRightDown);
            }
            //只有当左边和下边不全是障碍物，才能走左下的网格
            if ((childDown != null && !childDown.isInCloseList) || (childLeft != null && !childLeft.isInCloseList)) {
                OffRoadGrid childLeftDown = allList.stream().filter(
                        item -> item.x == nodeCurrent.x - 1 && item.y == nodeCurrent.y + 1
                ).findFirst().orElse(null);
                childList.add(childLeftDown);
            }
            //遍历周围网格
            for (int i = 0; i < childList.size(); i++) {
                OffRoadGrid child = childList.get(i);
                if (child == null || child.isInCloseList) {
                    //已经关闭，后面不再计算
                    continue;
                }
                //计算当前网格到它子网格的距离
                double currentToChild = Math.sqrt(
                        Math.pow(nodeCurrent.x - child.x, 2) +
                                Math.pow(nodeCurrent.y - child.y, 2)
                );
                if (!child.isInOpenList) {
                    //从来没有被计算过，现在计算它的代价
                    child.parentId = nodeCurrent.id;
                    //子网格的历史代价是当前网格历史代价加上当前网格到子网格的距离
                    child.g = nodeCurrent.g + currentToChild;
                    //子网格的未来期望代价是子网格到终点的距离
                    child.h = Math.sqrt(
                            Math.pow(child.x - this.endGrid.x, 2) + Math.pow(child.y - this.endGrid.y, 2)
                    );
                    //得出最终代价
                    child.f = child.g + child.h;
                    //设置标记，表明这个子网格已经被计算过至少一次了
                    child.isInOpenList = true;
                    openList.add(child); //将这个子网格加入到待计算列表中
                } else if (child.isInOpenList) {
                    //这个子网格被计算过
                    // 将子网格的父级替换为当前网格重新计算代价
                    double g = nodeCurrent.g + currentToChild;
                    //如果更换为新父级后代价比以前小，就更新一下
                    if (g < child.g) {
                        child.g = g;
                        child.f = child.g + child.h;
                        child.parentId = nodeCurrent.id;
                    }
                }
                //找到终点了，赋值后直接跳出
                if (child.x == this.endGrid.x && child.y == this.endGrid.y) {
                    endNode = child;
                    //溯源出路线
                    OffRoadGrid currentNode = endNode;
                    while (currentNode != null) {
                        roadPath.add(currentNode.center);
                        OffRoadGrid tmpNode = currentNode;
                        currentNode = allList.stream().filter(item -> item.id == tmpNode.parentId).findFirst().orElse(null);
                    }
                    break;

                }
            }
            if (endNode != null) {
                break;
            }

            //将当前网格从待计算列表中移除并将它关闭
            int index = 0;
            for (int i = 0; i < openList.size(); i++) {
                OffRoadGrid currentNode = openList.get(i);
                if (currentNode.x == nodeCurrent.x && currentNode.y == nodeCurrent.y) {
                    index = i;
                    break;
                }
            }
            openList.get(index).isInCloseList = true;
            openList.remove(index);
        }

        offRoadResponse.addOffRoadPoints(roadPath);
        return offRoadResponse;
    }
}
