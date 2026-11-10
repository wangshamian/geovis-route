package org.heigit.geovisroute.fastisochrones;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;

/**
 * 允许给定单元内的所有边以及到边界节点的所有边
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class CellAndBorderNodeFilter implements EdgeFilter {
    private final int maxNodes;
    private final IsochroneNodeStorage isochroneNodeStorage;
    private int cellId;

    /* 边位于指定单元内 */
    public CellAndBorderNodeFilter(IsochroneNodeStorage isochroneNodeStorage, int cellId, int maxNodes) {
        this.isochroneNodeStorage = isochroneNodeStorage;
        this.cellId = cellId;
        this.maxNodes = maxNodes - 1;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    @Override
    public final boolean accept(EdgeIteratorState iter) {
        int base = iter.getBaseNode();
        int adj = iter.getAdjNode();
        if (base >= maxNodes || adj >= maxNodes)
            return true;
        if (isochroneNodeStorage.getCellId(base) == cellId
                && isochroneNodeStorage.getCellId(adj) == cellId) {
            return true;
        }
        return isochroneNodeStorage.getBorderness(adj);
    }
}
