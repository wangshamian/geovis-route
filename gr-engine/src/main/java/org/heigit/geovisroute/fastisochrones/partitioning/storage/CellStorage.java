/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.heigit.geovisroute.fastisochrones.partitioning.storage;

import com.carrotsearch.hppc.*;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Storable;
import com.graphhopper.util.Helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.isSupercellsEnabled;
import static org.heigit.geovisroute.fastisochrones.storage.ByteConversion.byteArrayToLong;
import static org.heigit.geovisroute.fastisochrones.storage.ByteConversion.longToByteArray;

/**
 * 按单元存储节点以及单元的轮廓。
 *
 * @author Hendrik Leuschner
 */
public class CellStorage implements Storable<CellStorage> {
    private final DataAccess cells;
    private final int byteCount;
    private int nodeIndexOffset;
    private int contourIndexOffset;
    private final int nodeCount;
    private long cellContourPointer;
    private final IsochroneNodeStorage isochroneNodeStorage;
    private IntLongMap cellIdToNodesPointerMap;
    private IntLongMap cellIdToContourPointerMap;
    private IntIntMap cellIdToSuperCellMap = new IntIntHashMap();
    private IntObjectMap<IntHashSet> superCellIdToCellsMap = new IntObjectHashMap<>();

    /**
     * 创建新的单元存储。
     *
     * @param dir                  目录
     * @param isochroneNodeStorage 等时圈节点存储
     */
    public CellStorage(int nodeCount, Directory dir, IsochroneNodeStorage isochroneNodeStorage) {
        this.isochroneNodeStorage = isochroneNodeStorage;
        cells = dir.create("cells");
        byteCount = 4;
        this.nodeCount = nodeCount;
    }

    public boolean loadExisting() {
        if (cells.loadExisting()) {
            int cellCount = cells.getHeader(0);
            nodeIndexOffset = cellCount * 12;
            contourIndexOffset = 2 * cellCount * 18;
            cellIdToNodesPointerMap = new IntLongHashMap(cellCount);
            cellIdToContourPointerMap = new IntLongHashMap(cellCount);
            fillCellIdToNodesPointerMap();
            fillCellIdToContourPointerMap();
            if (isSupercellsEnabled()) {
                fillSuperCellMap();
                fillCellIdToSuperCellMap();
            }
            return true;
        }
        return false;
    }

    /**
     * 初始化。
     */
    public void init() {
        cells.create(1000);
        int cellCount = isochroneNodeStorage.getCellIds().size();
        cellIdToNodesPointerMap = new IntLongHashMap(cellCount);
        cellIdToContourPointerMap = new IntLongHashMap(cellCount);
        cellIdToSuperCellMap = new IntIntHashMap(cellCount);
    }

    /**
     * 遍历图中所有节点，创建单元 -> nodeId 的映射。
     * 创建 cellId -> 指针的映射，因为每个单元的 NodeId 数量不同，且存储是不规则的。
     * 将这两个映射都存储到存储中。
     */
    public void calcCellNodesMap() {
        IntObjectMap<IntHashSet> cellIdToNodesMap = new IntObjectHashMap<>(isochroneNodeStorage.getCellIds().size());
        //计算单元的哈希映射
        for (int node = 0; node < nodeCount; node++) {
            int cellId = isochroneNodeStorage.getCellId(node);
            if (!cellIdToNodesMap.containsKey(cellId))
                cellIdToNodesMap.put(cellId, new IntHashSet());
            cellIdToNodesMap.get(cellId).add(node);
        }
        int cellCount = cellIdToNodesMap.size();
        cells.setHeader(0, cellCount);
        // 每个 cellId 有两组 12 字节的指针
        // 在前部存储指针，指向每个单元的节点起始位置
        nodeIndexOffset = cellCount * 12;
        //由于存在超级单元轮廓，轮廓数量多于单元数量
        contourIndexOffset = 2 * cellCount * 18;
        long nodePointer = contourIndexOffset;

        //将所有单元节点存储到存储中
        for (IntCursor cellId : cellIdToNodesMap.keys()) {
            cells.ensureCapacity(nodePointer + (long) (cellIdToNodesMap.get(cellId.value).size() + 1) * byteCount);
            cellIdToNodesPointerMap.put(cellId.value, nodePointer);
            for (IntCursor nodeId : cellIdToNodesMap.get(cellId.value)) {
                cells.setInt(nodePointer, nodeId.value);
                nodePointer = nodePointer + (long) byteCount;
            }
            //添加末尾的 -1，以便知道何时停止
            cells.setInt(nodePointer, -1);
            nodePointer = nodePointer + (long) byteCount;
        }
        //将轮廓节点指针设置为节点部分的末尾
        cellContourPointer = nodePointer;

        //将 cellId 到指针的映射存储到存储中
        //布局：[cellId (4B), 节点指针 (8B)]
        long listPointer = 0;
        for (IntCursor cellId : cellIdToNodesPointerMap.keys()) {
            cells.setInt(listPointer, cellId.value);
            listPointer = listPointer + (long) byteCount;
            nodePointer = cellIdToNodesPointerMap.get(cellId.value);
            cells.setBytes(listPointer, longToByteArray(nodePointer), 8);
            listPointer = listPointer + (long) byteCount + (long) byteCount;
        }
    }

    /**
     * 获取单元节点的 IntHashSet。
     *
     * @param cellId 单元 ID
     * @return IntHashSet
     */
    public IntHashSet getNodesOfCell(int cellId) {
        if (cellIdToNodesPointerMap.isEmpty())
            throw new IllegalStateException("CellStorage not filled yet. Was calcCellNodesMap run?");
        long nodePointer = cellIdToNodesPointerMap.get(cellId);
        int currentNode = cells.getInt(nodePointer);
        IntHashSet nodeIds = new IntHashSet();
        while (currentNode != -1) {
            nodeIds.add(currentNode);
            nodePointer = nodePointer + (long) byteCount;
            currentNode = cells.getInt(nodePointer);
        }
        return nodeIds;
    }

    /**
     * 设置单元轮廓顺序。
     *
     * @param cellId     单元 ID
     * @param latitudes  纬度
     * @param longitudes 经度
     */
    public void setCellContourOrder(int cellId, List<Double> latitudes, List<Double> longitudes) {
        if (latitudes.size() != longitudes.size())
            throw new IllegalStateException("lat and lon must be same size");
        cellIdToContourPointerMap.put(cellId, cellContourPointer);
        cells.ensureCapacity(cellContourPointer + (long) 8 * (latitudes.size() + 1));
        for (int i = 0; i < latitudes.size(); i++) {
            cells.setInt(cellContourPointer, Helper.degreeToInt(latitudes.get(i)));
            cellContourPointer = cellContourPointer + (long) byteCount;
            cells.setInt(cellContourPointer, Helper.degreeToInt(longitudes.get(i)));
            cellContourPointer = cellContourPointer + (long) byteCount;
        }
        //添加末尾的 int 最大值，以便知道何时停止
        cells.setInt(cellContourPointer, Integer.MAX_VALUE);
        cellContourPointer = cellContourPointer + (long) byteCount;
        cells.setInt(cellContourPointer, Integer.MAX_VALUE);
        cellContourPointer = cellContourPointer + (long) byteCount;
    }

    /**
     * 获取单元轮廓顺序列表。
     *
     * @param cellId 单元 ID
     * @return 列表
     */
    public List<Double> getCellContourOrder(int cellId) {
        if (cellIdToContourPointerMap.isEmpty())
            throw new IllegalStateException("Cell contours not stored yet.");
        List<Double> order = new ArrayList<>();
        long nodePointer = cellIdToContourPointerMap.get(cellId);

        double lat = Helper.intToDegree(cells.getInt(nodePointer));
        nodePointer = nodePointer + (long) (byteCount);
        double lon = Helper.intToDegree(cells.getInt(nodePointer));
        while (cells.getInt(nodePointer) != Integer.MAX_VALUE) {
            order.add(lat);
            order.add(lon);
            nodePointer = nodePointer + (long) (byteCount);
            lat = Helper.intToDegree(cells.getInt(nodePointer));
            nodePointer = nodePointer + (long) (byteCount);
            lon = Helper.intToDegree(cells.getInt(nodePointer));
        }
        return order;
    }

    /**
     * 获取超级单元的单元 IntHashSet。
     *
     * @param superCell 超级单元
     * @return IntHashSet
     */
    public IntHashSet getCellsOfSuperCell(int superCell) {
        return superCellIdToCellsMap.get(superCell);
    }

    /**
     * 以列表形式获取超级单元的单元。
     *
     * @param superCell 超级单元
     * @return 列表
     */
    public List<Integer> getCellsOfSuperCellAsList(int superCell) {
        if (superCellIdToCellsMap.isEmpty())
            throw new IllegalStateException("Supercells not calculated yet.");
        return Arrays.stream(superCellIdToCellsMap.get(superCell).toArray()).boxed().collect(Collectors.toList());
    }

    /**
     * 获取单元的超级单元 ID。
     *
     * @param cell 单元
     * @return int
     */
    public int getSuperCellOfCell(int cell) {
        return cellIdToSuperCellMap.getOrDefault(cell, -1);
    }

    /**
     * 存储轮廓指针映射。
     */
    public void storeContourPointerMap() {
        long listPointer = nodeIndexOffset;
        long nodePointer;
        //存储轮廓数量（= 单元数 + 超级单元数）
        cells.setHeader(4, cellIdToContourPointerMap.keys().size());
        for (IntCursor cellId : cellIdToContourPointerMap.keys()) {
            cells.setInt(listPointer, cellId.value);
            listPointer = listPointer + (long) byteCount;
            nodePointer = cellIdToContourPointerMap.get(cellId.value);
            cells.setBytes(listPointer, longToByteArray(nodePointer), 8);
            listPointer = listPointer + (long) byteCount + (long) byteCount;
        }
    }

    /**
     * 存储超级单元。它是位于所有其他数据末尾的一个块，存储 [superCellId0, subcell0, subcell1, ..., -1, superCellId1, subcell0, subcell1, ..., -1, ..., -1]
     * 每个超级单元块以 -1 作为结束标记，并且整个块末尾再附加一个 -1。
     *
     * @param superCells 超级单元
     */
    public void storeSuperCells(IntObjectMap<IntHashSet> superCells) {
        //存储超级单元信息的起始位置
        superCellIdToCellsMap = superCells;
        cells.setHeader(8, (int) (cellContourPointer >> 32));
        cells.setHeader(12, (int) cellContourPointer);
        for (IntObjectCursor<IntHashSet> superCell : superCells) {
            // +1 用于 supercellId，+1 用于末尾的 -1
            cells.ensureCapacity(cellContourPointer + (long) (superCell.value.size() + 2) * byteCount);
            cells.setInt(cellContourPointer, superCell.key);
            cellContourPointer = cellContourPointer + (long) byteCount;
            for (IntCursor cellId : superCell.value) {
                cells.setInt(cellContourPointer, cellId.value);
                cellIdToSuperCellMap.put(cellId.value, superCell.key);
                cellContourPointer = cellContourPointer + (long) byteCount;
            }
            //添加末尾的 -1，以标示下一项
            cells.setInt(cellContourPointer, -1);
            cellContourPointer = cellContourPointer + (long) byteCount;
        }
        //添加第二个末尾 -1，标记 superCell 块的结束
        cells.ensureCapacity(cellContourPointer + (long) byteCount);
        cells.setInt(cellContourPointer, -1);
        cellContourPointer = cellContourPointer + (long) byteCount;
    }

    private void fillCellIdToNodesPointerMap() {
        int cellCount = cells.getHeader(0);
        byte[] buffer = new byte[8];

        for (int i = 0; i < cellCount; i++) {
            int cellId = cells.getInt((long) i * 12);
            cells.getBytes((long) i * 12 + 4, buffer, 8);
            long nodePointer = byteArrayToLong(buffer);
            cellIdToNodesPointerMap.put(cellId, nodePointer);
        }
    }

    private void fillCellIdToContourPointerMap() {
        int contourCount = cells.getHeader(4);
        byte[] buffer = new byte[8];
        long listPointer = nodeIndexOffset;
        for (int i = 0; i < contourCount; i++) {
            int cellId = cells.getInt(listPointer);
            listPointer = listPointer + (long) byteCount;

            cells.getBytes(listPointer, buffer, 8);
            long nodePointer = byteArrayToLong(buffer);
            listPointer = listPointer + (long) 8;
            cellIdToContourPointerMap.put(cellId, nodePointer);
        }
    }

    private void fillSuperCellMap() {
        long bytePos = (long) cells.getHeader(8) << 32 | cells.getHeader(12) & 0xFFFFFFFFL;
        while (cells.getInt(bytePos) != -1) {
            int superCellId = cells.getInt(bytePos);
            IntHashSet cellIds = new IntHashSet();
            bytePos += byteCount;
            while (cells.getInt(bytePos) != -1) {
                cellIds.add(cells.getInt(bytePos));
                bytePos += byteCount;
            }
            superCellIdToCellsMap.put(superCellId, cellIds);
            bytePos += byteCount;
        }
    }

    private void fillCellIdToSuperCellMap() {
        for (IntObjectCursor<IntHashSet> superCell : superCellIdToCellsMap) {
            for (IntCursor cellId : superCell.value) {
                cellIdToSuperCellMap.put(cellId.value, superCell.key);
            }
        }
    }

    /**
     * 轮廓是否已准备。
     *
     * @return boolean
     */
    public boolean isContourPrepared() {
        return cells.getHeader(16) > 0;
    }

    /**
     * 设置轮廓是否已准备。
     *
     * @param prepared 是否已准备
     */
    public void setContourPrepared(boolean prepared) {
        cells.setHeader(16, prepared ? 1 : 0);
    }

    public CellStorage create(long byteCount) {
        throw new IllegalStateException("Do not call CellStorage.create directly");
    }

    public void flush() {
        cells.flush();
    }

    @Override
    public void close() {
        cells.close();
    }

    @Override
    public boolean isClosed() {
        return cells.isClosed();
    }

    public long getCapacity() {
        return cells.getCapacity();
    }
}
