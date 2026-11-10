package org.heigit.geovisroute.routing.graphhopper.extensions.storages;

import com.graphhopper.storage.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.EncodeUtils;

public class OsmIdGraphStorage implements GraphExtension {
    /* 空条目的指针 */
    protected final int efOsmid;

    protected DataAccess orsEdges;
    protected int edgeEntryIndex = 0;
    protected int edgeEntryBytes;
    protected int edgesCount; // 带自定义值的边数

    private final byte[] byteValues;

    public OsmIdGraphStorage() {
        efOsmid = 0;
        edgeEntryBytes = edgeEntryIndex + 4;
        edgesCount = 0;
        byteValues = new byte[4];
    }

    public void init(Graph graph, Directory dir) {
        if (edgesCount > 0)
            throw new AssertionError("The ORS storage must be initialized only once.");

        this.orsEdges = dir.create("ext_osmids");
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

    public OsmIdGraphStorage create(long initBytes) {
        orsEdges.create(initBytes * edgeEntryBytes);
        return this;
    }

    public void flush() {
        orsEdges.setHeader(0, edgeEntryBytes);
        orsEdges.setHeader(4, edgesCount);
        orsEdges.flush();
    }

    public void close() {
        orsEdges.close();
    }

    @Override
    public long getCapacity() {
        return orsEdges.getCapacity();
    }

    public int entries() {
        return edgesCount;
    }

    public boolean loadExisting() {
        if (!orsEdges.loadExisting())
            throw new IllegalStateException("Unable to load storage 'ext_osmids'. corrupt file or directory? ");

        edgeEntryBytes = orsEdges.getHeader(0);
        edgesCount = orsEdges.getHeader(4);
        return true;
    }

    void ensureEdgesIndex(int edgeIndex) {
        orsEdges.ensureCapacity(((long) edgeIndex + 1) * edgeEntryBytes);
    }

    /**
     * 将边的 osm id 设置为指定值。
     *
     * @param edgeId 图中边的内部 id
     * @param osmId  要分配给该边的 osm id
     */
    public void setEdgeValue(int edgeId, long osmId) {
        edgesCount++;
        ensureEdgesIndex(edgeId);

        // 添加条目
        long edgePointer = (long) edgeId * edgeEntryBytes;
        byte[] tempBytes = EncodeUtils.longToByteArray(osmId);
        byteValues[0] = tempBytes[4];
        byteValues[1] = tempBytes[5];
        byteValues[2] = tempBytes[6];
        byteValues[3] = tempBytes[7];
        orsEdges.setBytes(edgePointer + efOsmid, byteValues, 4);
    }

    /**
     * 获取指定边的 OSM id
     *
     * @param edgeId 需要获取 OSM way ID 的边的内部图 id
     * @return 为该边存储的 OSM ID（通常是生成该边的 OSM way 的 ID）
     */
    public long getEdgeValue(int edgeId) {
        byte[] buffer = new byte[4];
        long edgePointer = (long) edgeId * edgeEntryBytes;
        orsEdges.getBytes(edgePointer + efOsmid, buffer, 4);

        return EncodeUtils.byteArrayToLong(buffer);
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
