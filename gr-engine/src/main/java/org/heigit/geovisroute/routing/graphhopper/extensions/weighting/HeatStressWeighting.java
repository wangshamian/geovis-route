package org.heigit.geovisroute.routing.graphhopper.extensions.weighting;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.querygraph.EdgeIteratorStateHelper;
import com.graphhopper.routing.weighting.SpeedWeighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.CsvGraphStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;

public class HeatStressWeighting extends SpeedWeighting {

    private final CsvGraphStorage heatStressStorage;
    private final byte[] buffer;
    private final double weightingFactor;
    private final String columnName;
    private final int columnIndex; // 出于性能考虑缓存 columnName 的索引

    public HeatStressWeighting(DecimalEncodedValue speedEnc, PMap map, BaseGraph basegraph) {
        super(speedEnc);
        heatStressStorage = GraphStorageUtils.getGraphExtension(basegraph, CsvGraphStorage.class);
        buffer = new byte[heatStressStorage.numEntries()];

        weightingFactor = map.getDouble("factor", 1);
        this.columnName = map.getString("column", "");
        this.columnIndex = heatStressStorage.columnIndex(columnName);
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        // TODO: 这是一个短期方案，待
        //       AdditionWeighting 被替换时需要进行调整
        if (heatStressStorage != null) {
            int stressLevel = heatStressStorage.getEdgeValue(EdgeIteratorStateHelper.getOriginalEdge(edgeState), columnIndex, buffer);
            return stressLevel * weightingFactor / 100 + 1.0;
        }

        return 1.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final HeatStressWeighting other = (HeatStressWeighting) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return ("HeatStressWeighting" + this).hashCode();
    }
}
