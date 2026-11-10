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
package org.heigit.geovisroute.routing.graphhopper.extensions.storages;

import com.graphhopper.storage.*;

/**
 * 用于边界限制路由的图存储类
 */
public class BordersGraphStorage implements GraphExtension {
    public enum Property {TYPE, START, END}

    /* 空条目的指针 */
    protected static final int NO_ENTRY = -1;
    private static final int EF_BORDER = 0;        // 边界类型的字节位置
    private static final int EF_START = 2;            // 起始国家 id 的字节位置
    private static final int EF_END = 4;            // 结束国家 id 的字节位置

    // 边界类型
    public static final short NO_BORDER = 0;
    public static final short OPEN_BORDER = 2;
    public static final short CONTROLLED_BORDER = 1;

    private DataAccess orsEdges;
    private int edgeEntryBytes;
    private int edgesCount; // 带自定义值的边数

    public BordersGraphStorage() {

        int edgeEntryIndex = 0;
        edgeEntryBytes = edgeEntryIndex + 6;    // 每个条目使用 3 个 short 值，各占 2 字节
        edgesCount = 0;
    }

    /**
     * 根据边界类型和国家为边设置值<br/><br/>
     * <p>
     * 该方法接收边的内部 ID，并将从边界 CSV 文件中获得的信息添加进去，
     * 以便在生成路线时考虑这些值。
     *
     * @param edgeId     图边的内部 ID
     * @param borderType 边界穿越级别（0 - 无边界，1 - 受控边界，2 - 开放边界）
     * @param start      边起始所在国家的 ID
     * @param end        边结束所在国家的 ID
     */
    public void setEdgeValue(int edgeId, short borderType, short start, short end) {
        edgesCount++;
        ensureEdgesIndex(edgeId);

        // 添加条目
        long edgePointer = (long) edgeId * edgeEntryBytes;

        orsEdges.setShort(edgePointer + EF_BORDER, borderType);
        orsEdges.setShort(edgePointer + EF_START, start);
        orsEdges.setShort(edgePointer + EF_END, end);
    }

    private void ensureEdgesIndex(int edgeId) {
        orsEdges.ensureCapacity(((long) edgeId + 1) * edgeEntryBytes);
    }

    /**
     * 获取在 setValueEdge 方法中为边指定的自定义值<br/><br/>
     * <p>
     * 该方法接收边的标识符，然后从存储中获取该边所请求的值
     *
     * @param edgeId 要获取值的边的内部 ID
     * @param prop   要获取的边属性（TYPE - 边界类型（0,1,2），START - 边起始所在国家的 ID，
     *               END - 边结束所在国家的 ID。
     * @return 所请求属性的值
     */
    public short getEdgeValue(int edgeId, Property prop) {
        long edgePointer = (long) edgeId * edgeEntryBytes;
        short border = orsEdges.getShort(edgePointer + EF_BORDER);
        short start = orsEdges.getShort(edgePointer + EF_START);
        short end = orsEdges.getShort(edgePointer + EF_END);

        return switch (prop) {
            case TYPE -> border;
            case START -> start;
            case END -> end;
            default -> 0;
        };

    }

    /**
     * 通过传入基础图来初始化扩展存储
     *
     * @param graph
     * @param dir
     */
    @Override
    public void init(Graph graph, Directory dir) {
        if (edgesCount > 0)
            throw new AssertionError("The ORS storage must be initialized only once.");

        this.orsEdges = dir.create("ext_borders");
    }

    /**
     * 将扩展存储初始化为空 - 这是测试所需，因为 ext_storage 在运行测试时并不会创建
     */
    public void init() {
        if (edgesCount > 0)
            throw new AssertionError("The ORS storage must be initialized only once.");
        Directory d = new RAMDirectory();
        this.orsEdges = d.create("");
    }

    /**
     * @return 若从持久化存储成功加载则返回 true。
     */
    @Override
    public boolean loadExisting() {
        if (!orsEdges.loadExisting())
            throw new IllegalStateException("Unable to load storage 'ext_borders'. corrupt file or directory?");

        edgeEntryBytes = orsEdges.getHeader(0);
        edgesCount = orsEdges.getHeader(4);
        return true;
    }

    /**
     * 创建底层存储。若无法加载，则作为第一个操作执行。
     *
     * @param initBytes
     */
    @Override
    public BordersGraphStorage create(long initBytes) {
        orsEdges.create(initBytes * edgeEntryBytes);
        return this;
    }

    /**
     * 此方法确保底层数据被写入存储。请注意，磁盘通常有 IO 缓存，
     * 因此 flush() 可能（较小的可能）无法防止断电丢失。
     */
    @Override
    public void flush() {
        orsEdges.setHeader(0, edgeEntryBytes);
        orsEdges.setHeader(4, edgesCount);
        orsEdges.flush();
    }

    /**
     * 此方法确保释放底层使用的资源。警告：关闭时它并<b>不会</b>
     * 执行 flush！
     */
    @Override
    public void close() {
        orsEdges.close();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    /**
     * @return 已分配的存储大小（字节）
     */
    @Override
    public long getCapacity() {
        return orsEdges.getCapacity();
    }
}
