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
import com.graphhopper.routing.weighting.TurnCostProvider;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;

/**
 * 修改版的 `FastestWeighting`，去除了对 access 为 destination 或 private 的边的惩罚。
 */
public class GRFastestWeighting extends SpeedWeighting {
    protected final static double SPEED_CONV = 3.6;
    private final double headingPenalty;

    public GRFastestWeighting(DecimalEncodedValue speedEnc) {
        this(speedEnc, new PMap(0), TurnCostProvider.NO_TURN_COST_PROVIDER);
    }

    public GRFastestWeighting(DecimalEncodedValue speedEnc, PMap map, TurnCostProvider turnCostProvider) {
        super(speedEnc, turnCostProvider);
        headingPenalty = map.getDouble(Parameters.Routing.HEADING_PENALTY, Parameters.Routing.DEFAULT_HEADING_PENALTY);
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse, long edgeEnterTime) {
        double speed = reverse ? edgeState.getReverse(speedEnc) : edgeState.get(speedEnc);
        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        double time = edgeState.getDistance() / speed * SPEED_CONV;

        // 在起点/停靠点/途经点增加方向惩罚
        boolean unfavoredEdge = edgeState.get(EdgeIteratorState.UNFAVORED_EDGE);
        if (unfavoredEdge)
            time += headingPenalty;

        return time;
    }

}
