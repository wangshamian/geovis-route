package org.heigit.geovisroute.routing.graphhopper.extensions.reader.osmfeatureprocessors;

import com.graphhopper.reader.ReaderWay;


public class PedestrianSidewalkWay extends PedestrianWay {
    private final OSMAttachedSidewalkProcessor sidewalkProcessor;
    private final OSMAttachedSidewalkProcessor.Side side;
    private OSMAttachedSidewalkProcessor.Side lastPrepared = OSMAttachedSidewalkProcessor.Side.NONE;

    public PedestrianSidewalkWay(ReaderWay way) {
        super(way);

        sidewalkProcessor = new OSMAttachedSidewalkProcessor();

        this.side = sidewalkProcessor.identifySidesWhereSidewalkIsPresent(this.readerWay);
    }

    @Override
    public boolean hasWayBeenFullyProcessed() {
        if (side == OSMAttachedSidewalkProcessor.Side.BOTH && lastPrepared == OSMAttachedSidewalkProcessor.Side.RIGHT) {
            return true;
        }

        return side == lastPrepared;
    }

    @Override
    public void prepare() {
        // 检查是否已经处理过任何内容

        if (side == OSMAttachedSidewalkProcessor.Side.BOTH && lastPrepared == OSMAttachedSidewalkProcessor.Side.RIGHT) {
            // 当处理两侧时，右侧人行道是最后处理的
            lastPrepared = OSMAttachedSidewalkProcessor.Side.BOTH;
        }

        if (lastPrepared != side) {
            this.readerWay = sidewalkProcessor.attachSidewalkTag(this.readerWay, side);
            lastPrepared = sidewalkProcessor.getPreparedSide(this.readerWay);
        }
    }
}
