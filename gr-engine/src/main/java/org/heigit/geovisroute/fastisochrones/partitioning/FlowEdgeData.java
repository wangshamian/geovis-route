package org.heigit.geovisroute.fastisochrones.partitioning;

/**
 * 用于分区的数据元素。
 *
 * @author Hendrik Leuschner
 */
public class FlowEdgeData {
    private boolean flow;
    private int inverse;

    public FlowEdgeData(boolean flow, int inverse) {
        this.flow = flow;
        this.inverse = inverse;
    }

    public boolean isFlow() {
        return flow;
    }

    public void setFlow(boolean flow) {
        this.flow = flow;
    }

    public int getInverse() {
        return inverse;
    }

    public void setInverse(int inverse) {
        this.inverse = inverse;
    }
}
