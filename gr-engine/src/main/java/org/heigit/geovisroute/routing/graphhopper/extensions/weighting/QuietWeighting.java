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
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.NoiseIndexGraphStorage;

public class QuietWeighting extends SpeedWeighting {
    private final NoiseIndexGraphStorage gsNoiseIndex;
    private final byte[] buffer;
    private double weightingFactor = 1;

    public QuietWeighting(DecimalEncodedValue speedEnc, PMap map, BaseGraph basegraph) {
        super(speedEnc);
        buffer = new byte[1];
        gsNoiseIndex = GraphStorageUtils.getGraphExtension(basegraph, NoiseIndexGraphStorage.class);
        weightingFactor = map.getDouble("factor", 1);
    }

    private double calcNoiseWeightFactor(int level) {
        if (level == 0)
            return 1;
        else if (level <= 1)
            return 1 + weightingFactor * 10;
        else if (level <= 2)
            return 1 + weightingFactor * weightingFactor * 200;  // 对噪声等级 2 和 3 大幅降低系数，但仍大于噪声等级 1 的系数
        else if (level <= 3)
            return 1 + weightingFactor * weightingFactor * 400;
        else
            throw new AssertionError("The noise level " + level + " is not supported!");
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        if (gsNoiseIndex != null) {
            int noiseLevel = gsNoiseIndex.getEdgeValue(EdgeIteratorStateHelper.getOriginalEdge(edgeState), buffer);
            return calcNoiseWeightFactor(noiseLevel);
        }

        return 1.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final QuietWeighting other = (QuietWeighting) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return ("QuietWeighting" + this).hashCode();
    }
}