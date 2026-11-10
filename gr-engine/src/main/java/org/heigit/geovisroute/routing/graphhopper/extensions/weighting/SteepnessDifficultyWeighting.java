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
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.HillIndexGraphStorage;

public class SteepnessDifficultyWeighting extends SpeedWeighting {

    private final HillIndexGraphStorage gsHillIndex;
    private final byte[] buffer;
    private double[] difficultyWeights;

    private static final double[][] BIKE_DIFFICULTY_MATRIX = { // [4][20]
            {0.5, 0.5, 0.5, 0.7, 0.9, 1.5, 3, 3.5, 4, 5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5},
            {0.7, 0.6, 0.6, 0.5, 0.5, 0.8, 1.0, 2, 3, 4, 5, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5},
            {1.6, 1.6, 1.5, 1.5, 0.7, 0.5, 0.5, 0.5, 1, 2, 2.5, 2.5, 3, 4, 5, 7.5, 7.6, 7.7, 7.8, 7.9},
            {1.6, 1.6, 1.5, 1.5, 0.9, 0.7, 0.5, 0.5, 0.6, 0.7, 0.9, 1.2, 2, 3, 5, 6, 7.7, 7.8, 7.9, 8.0}
    };

    public SteepnessDifficultyWeighting(DecimalEncodedValue speedEnc, PMap map, BaseGraph basegraph) {
        super(speedEnc);
        buffer = new byte[1];
        int difficultyLevel = map.getInt("level", -1);
        gsHillIndex = GraphStorageUtils.getGraphExtension(basegraph, HillIndexGraphStorage.class);
        // TODO: 检查 difficultyLevel 的上界。这里正确的行为是什么？
        // TODO: 像处理负值那样忽略，还是抛出专用异常？
        if (gsHillIndex != null && difficultyLevel >= 0) {
            difficultyWeights = BIKE_DIFFICULTY_MATRIX[difficultyLevel];
        }
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        if (gsHillIndex != null) {
            boolean revert = edgeState.getBaseNode() < edgeState.getAdjNode();
            int hillIndex = gsHillIndex.getEdgeValue(EdgeIteratorStateHelper.getOriginalEdge(edgeState), revert, buffer);

            if (difficultyWeights != null) {
                // TODO: 明确是否需要检查 hillIndex 越界。
                return difficultyWeights[hillIndex];
            }
        }
        return 1.0;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: 明确 equals 是否应取决于难度等级。
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SteepnessDifficultyWeighting other = (SteepnessDifficultyWeighting) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        // TODO: 明确 hashCode 是否应取决于难度等级。
        return ("SteepnessDifficultyWeighting" + this).hashCode();
    }
}
