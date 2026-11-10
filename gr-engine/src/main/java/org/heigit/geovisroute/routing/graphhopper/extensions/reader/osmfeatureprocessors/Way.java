package org.heigit.geovisroute.routing.graphhopper.extensions.reader.osmfeatureprocessors;

import com.graphhopper.reader.ReaderWay;

public abstract class Way {
    protected ReaderWay readerWay;

    public ReaderWay getReaderWay() {
        return this.readerWay;
    }

    /**
     * 该 way 是否为步行化道路
     *
     * @return
     */
    public boolean isPedestrianised() {
        return false;
    }

    /**
     * 判断该 way 是否已完全处理完毕
     *
     * @return
     */
    public abstract boolean hasWayBeenFullyProcessed();

    /**
     * 为处理准备好该 way，例如添加侧边 tags
     */
    public void prepare() {
    }
}
