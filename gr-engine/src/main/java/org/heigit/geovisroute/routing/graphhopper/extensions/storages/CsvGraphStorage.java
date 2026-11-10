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

    import com.graphhopper.storage.DataAccess;
    import com.graphhopper.storage.Directory;
    import com.graphhopper.storage.Graph;
    import com.graphhopper.storage.GraphExtension;

    import java.util.Arrays;

    public class CsvGraphStorage implements GraphExtension {
        /* 空条目的指针 */
        private final int efCsvIndex; // TODO: 这是什么？

        private DataAccess orsEdges;
        private int edgeEntryBytes;
        private int edgesCount; // 带自定义值的边数
        private final int numEntries;
        private final String[] columnNames;

        public CsvGraphStorage(String[] columnNames) {
            efCsvIndex = 0;
            this.columnNames = columnNames;
            numEntries = columnNames.length;

            int edgeEntryIndex = 0;
            edgeEntryBytes = edgeEntryIndex + numEntries;
            edgesCount = 0;
        }

        public void setEdgeValue(int edgeId, byte[] values) {
            edgesCount++;
            ensureEdgesIndex(edgeId);

            // 添加条目
            long edgePointer = (long) edgeId * edgeEntryBytes;
            orsEdges.setBytes(edgePointer + efCsvIndex, values, numEntries);
        }

        private void ensureEdgesIndex(int edgeId) {
            orsEdges.ensureCapacity(((long) edgeId + 1) * edgeEntryBytes);
        }

        public int getEdgeValue(int edgeId, int columnIndex, byte[] buffer) {
            long edgePointer = (long) edgeId * edgeEntryBytes;
            // TODO: 也许我们不需要获取整个缓冲区，而只需获取
            //       单个元素，并在调用处使用单元素缓冲区
            orsEdges.getBytes(edgePointer + efCsvIndex, buffer, numEntries);

            return buffer[columnIndex];
        }

        public int columnIndex(String columnName) {
            int index = 0;
            for (String name : columnNames) {
                if (name.equals(columnName)) {
                    return index;
                }
                index++;
            }
            throw new IllegalArgumentException("Illegal column name: " + columnName);
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

            this.orsEdges = dir.create("ext_csv");
        }

        /**
         * @return 若从持久化存储成功加载则返回 true。
         */
        @Override
        public boolean loadExisting() {
            if (!orsEdges.loadExisting())
                throw new IllegalStateException("Unable to load storage 'ext_csv'. corrupt file or directory?");

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
        public GraphExtension create(long initBytes) {
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

        public String[] columnNames() {
            return Arrays.copyOf(columnNames, columnNames.length);
        }

        public int numEntries() {
            return numEntries;
        }
    }


