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
package org.heigit.geovisroute.fastisochrones.storage;

import com.carrotsearch.hppc.IntLongHashMap;
import com.carrotsearch.hppc.cursors.IntLongCursor;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Storable;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.util.FileUtility;

import static org.heigit.geovisroute.fastisochrones.storage.ByteConversion.byteArrayToLong;
import static org.heigit.geovisroute.fastisochrones.storage.ByteConversion.longToByteArray;

/**
 * 存储快速等时圈中单元边界节点的偏心度。偏心度依赖于权重，因此与单元分开存储。
 *
 * @author Hendrik Leuschner
 */
public class EccentricityStorage implements Storable<EccentricityStorage> {
    private final DataAccess eccentricities;
    private final int eccentricityBytes;
    private final int mapBytes;
    private final int eccentricityPosition;
    private final int nodeCount;
    private final Weighting weighting;
    private final IsochroneNodeStorage isochroneNodeStorage;
    private int borderNodeIndexOffset;
    private int borderNodePointer;
    private IntLongHashMap borderNodeToPointerMap;
    private int borderNodeCount;

    /**
     * 创建新的偏心度存储。
     *
     * @param dir                  目录
     * @param weighting            权重
     * @param isochroneNodeStorage 等时圈节点存储
     */
    public EccentricityStorage(Directory dir, Weighting weighting, IsochroneNodeStorage isochroneNodeStorage, int nodeCount) {
        //第一个块中存储 nodeId 到指针的映射。
        //第二个块为每个指针存储 2 个值：完全可达性和偏心度
        final String name = FileUtility.weightingToFileName(weighting);
        eccentricities = dir.create("eccentricities_" + name);
        this.weighting = weighting;
        this.isochroneNodeStorage = isochroneNodeStorage;
        this.nodeCount = nodeCount;
        //  每个节点 2 个 int，第一个是完全可达性，第二个是偏心度
        this.eccentricityBytes = 8;
        this.mapBytes = 12;
        this.eccentricityPosition = 4;
    }

    public boolean loadExisting() {
        if (eccentricities.loadExisting()) {
            borderNodeCount = eccentricities.getHeader(0);
            borderNodeIndexOffset = borderNodeCount * mapBytes;
            borderNodePointer = borderNodeIndexOffset;
            borderNodeToPointerMap = new IntLongHashMap(borderNodeCount);
            loadBorderNodeToPointerMap();
            return true;
        }
        return false;
    }

    /**
     * 初始化。
     */
    public void init() {
        eccentricities.create(1000);
        borderNodeCount = getNumBorderNodes();
        eccentricities.setHeader(0, borderNodeCount);
        borderNodeIndexOffset = borderNodeCount * mapBytes;
        borderNodePointer = borderNodeIndexOffset;
        borderNodeToPointerMap = new IntLongHashMap();
        generateBorderNodeToPointerMap();
        eccentricities.ensureCapacity((long) borderNodeIndexOffset + (long) borderNodeCount * eccentricityBytes);
    }

    private int getNumBorderNodes() {
        int count = 0;
        for (int node = 0; node < nodeCount; node++) {
            if (isochroneNodeStorage.getBorderness(node)) {
                count++;
            }
        }
        return count;
    }

    private void generateBorderNodeToPointerMap() {
        for (int node = 0; node < nodeCount; node++) {
            if (isochroneNodeStorage.getBorderness(node)) {
                borderNodeToPointerMap.put(node, borderNodePointer);
                borderNodePointer += (long) eccentricityBytes;
            }
        }
    }

    /**
     * 设置偏心度。
     *
     * @param node         节点
     * @param eccentricity 偏心度
     */
    public void setEccentricity(int node, double eccentricity) {
        eccentricities.setInt(borderNodeToPointerMap.get(node) + eccentricityPosition, (int) Math.ceil(eccentricity));
    }

    /**
     * 获取偏心度。
     *
     * @param node 节点
     * @return 偏心度
     */
    public int getEccentricity(int node) {
        long index = borderNodeToPointerMap.get(node);
        if (index == 0)
            throw new IllegalArgumentException("Requested node is not a border node");
        return eccentricities.getInt(index + eccentricityPosition);
    }

    /**
     * 设置完全可达性。
     *
     * @param node             节点
     * @param isFullyReachable 是否完全可达
     */
    public void setFullyReachable(int node, boolean isFullyReachable) {
        if (isFullyReachable)
            eccentricities.setInt(borderNodeToPointerMap.get(node), 1);
        else
            eccentricities.setInt(borderNodeToPointerMap.get(node), 0);
    }

    /**
     * 获取完全可达性。
     *
     * @param node 节点
     * @return 完全可达性
     */
    public boolean getFullyReachable(int node) {
        int isFullyReachable = eccentricities.getInt(borderNodeToPointerMap.get(node));
        return isFullyReachable == 1;
    }

    /**
     * 存储边界节点到指针的映射。
     */
    public void storeBorderNodeToPointerMap() {
        long listPointer = 0;
        long nodePointer;
        for (IntLongCursor borderNode : borderNodeToPointerMap) {
            eccentricities.setInt(listPointer, borderNode.key);
            listPointer = listPointer + 4;
            nodePointer = borderNode.value;
            eccentricities.setBytes(listPointer, longToByteArray(nodePointer), 8);
            listPointer = listPointer + 8;
        }
    }

    private void loadBorderNodeToPointerMap() {
        byte[] buffer = new byte[8];
        long listPointer = 0;
        for (int i = 0; i < borderNodeCount; i++) {
            int borderNode = eccentricities.getInt(listPointer);
            listPointer = listPointer + 4;
            eccentricities.getBytes(listPointer, buffer, 8);
            long nodePointer = byteArrayToLong(buffer);
            listPointer = listPointer + (long) 8;
            borderNodeToPointerMap.put(borderNode, nodePointer);
        }
    }

    public EccentricityStorage create(long byteCount) {
        throw new IllegalStateException("Do not call EccentricityStorage.create directly");
    }

    public void flush() {
        eccentricities.flush();
    }

    @Override
    public void close() {
        eccentricities.close();
    }

    @Override
    public boolean isClosed() {
        return eccentricities.isClosed();
    }

    public long getCapacity() {
        return eccentricities.getCapacity();
    }

    /**
     * 获取权重。
     *
     * @return 权重
     */
    public Weighting getWeighting() {
        return weighting;
    }

    public boolean hasWeighting(Weighting weighting) {
        return getWeighting().getName() != null
                && getWeighting().getName().equals(weighting.getName())
                && getWeighting().toString() != null
                && getWeighting().toString().equals(weighting.toString());
    }
}
