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
package org.heigit.geovisroute.routing.graphhopper.extensions.weighting;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.querygraph.EdgeIteratorStateHelper;
import com.graphhopper.routing.weighting.SpeedWeighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GreenIndexGraphStorage;

/**
 * 由 lliu 于 2017/03/15 创建。
 */
public class GreenWeighting extends SpeedWeighting {
    private final GreenIndexGraphStorage gsGreenIndex;
    private final byte[] buffer = new byte[1];
    private final double[] factors = new double[TOTAL_LEVEL];

    private static final int TOTAL_LEVEL = 64;

    public GreenWeighting(DecimalEncodedValue speedEnc, PMap map, BaseGraph basegraph) {
        super(speedEnc);

        gsGreenIndex = GraphStorageUtils.getGraphExtension(basegraph, GreenIndexGraphStorage.class);
        double factor = map.getDouble("factor", 1);

        for (int i = 0; i < TOTAL_LEVEL; i++)
            factors[i] = calcGreenWeightFactor(i, factor);
    }

    private double calcGreenWeightFactor(int level, double factor) {
        // 这里有一个隐含约定：
        // 绿色等级范围为 [0, total - 1]。
        // @level 会被线性变换为一个落在 (0, 2] 区间内的浮点数
        // 不过，对于最终的绿色权重，
        // 还会引入一个权重因子
        // 来控制“绿色考虑”的影响程度，
        // 就像放大器一样
        double wf = (double) (level + 1) * 2.0 / TOTAL_LEVEL;
        return 1.0 - (1.0 - wf) * factor;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        if (gsGreenIndex != null) {
            int greenLevel = gsGreenIndex.getEdgeValue(EdgeIteratorStateHelper.getOriginalEdge(edgeState), buffer);
            return factors[greenLevel];
        }

        return 1.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final GreenWeighting other = (GreenWeighting) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return ("GreenWeighting" + this).hashCode();
    }
}
