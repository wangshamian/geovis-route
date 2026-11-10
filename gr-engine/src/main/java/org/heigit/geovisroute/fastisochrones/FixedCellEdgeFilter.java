package org.heigit.geovisroute.fastisochrones;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;

/**
 * 用于保持在单元内的 EdgeFilter。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class FixedCellEdgeFilter implements EdgeFilter {
    //任何 ID 高于此值的节点都是虚拟节点
    private final int maxNodes;
    private final IsochroneNodeStorage isochroneNodeStorage;
    private int cellId;
    private boolean strict = true;

    /* 边位于指定单元内 */
    public FixedCellEdgeFilter(IsochroneNodeStorage isochroneNodeStorage, int cellId, int maxNodes, boolean strict) {
        this.isochroneNodeStorage = isochroneNodeStorage;
        this.cellId = cellId;
        this.maxNodes = maxNodes;
        this.strict = strict;
    }

    public FixedCellEdgeFilter(IsochroneNodeStorage isochroneNodeStorage, int cellId, int maxNodes) {
        this(isochroneNodeStorage, cellId, maxNodes, true);
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    @Override
    public final boolean accept(EdgeIteratorState iter) {
        if (iter.getBaseNode() >= maxNodes || iter.getAdjNode() >= maxNodes)
            return true;
        if (strict) {
            return isochroneNodeStorage.getCellId(iter.getBaseNode()) == cellId
                    && isochroneNodeStorage.getCellId(iter.getAdjNode()) == cellId;
        }
        return isochroneNodeStorage.getCellId(iter.getBaseNode()) == cellId
                || isochroneNodeStorage.getCellId(iter.getAdjNode()) == cellId;
    }
}
