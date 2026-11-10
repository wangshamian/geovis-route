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
import com.graphhopper.routing.weighting.SpeedWeighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.PriorityCode;

/**
 * 针对（电动）自行车的特殊权重
 * <p>
 *
 * @author Peter Karich
 */
public class FastestSafeWeighting extends SpeedWeighting {
    private final Double priorityThreshold = PriorityCode.REACH_DEST.getValue() / (double) PriorityCode.BEST.getValue();

    public FastestSafeWeighting(DecimalEncodedValue speedEnc, PMap map, BaseGraph basegraph) {
        super(speedEnc);
    }

//    @Override
//    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse, long edgeEnterTime) {
//        double weight = super.calcEdgeWeight(edgeState, reverse, edgeEnterTime);
//        if (Double.isInfinite(weight))
//            return Double.POSITIVE_INFINITY;
//
//        double priority = speedEnc.getDecimal(reverse, 0, edgeState);
//
//        if (priority <= priorityThreshold)
//            weight *= 2;
//
//        return weight;
//    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final FastestSafeWeighting other = (FastestSafeWeighting) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return ("FastestSafeWeighting" + this).hashCode();
    }
}
