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
package org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.EdgeIteratorState;

/**
 * 该类将速度高于 gr-config.json 文件中 max_speed 所设值的所有边纳入 core。
 *
 * @author Athanasios Kogios
 */

public class MaximumSpeedCoreEdgeFilter implements EdgeFilter {
//    private final double maximumSpeedLowerBound;
//
//    private final DecimalEncodedValue avSpeedEnc;

//    public MaximumSpeedCoreEdgeFilter(String encodeName, double maximumSpeedLowerBound) {
    public MaximumSpeedCoreEdgeFilter() {
//        this.maximumSpeedLowerBound = maximumSpeedLowerBound;
//        this.avSpeedEnc = flagEncoder.getAverageSpeedEnc();
    }

    @Override
    public boolean accept(EdgeIteratorState edge) {
        //如果道路的最大速度大于限制值，则将其纳入 core。
//        return edge.get(avSpeedEnc) <= maximumSpeedLowerBound && edge.getReverse(avSpeedEnc) <= maximumSpeedLowerBound;
        return false;
    }
}
