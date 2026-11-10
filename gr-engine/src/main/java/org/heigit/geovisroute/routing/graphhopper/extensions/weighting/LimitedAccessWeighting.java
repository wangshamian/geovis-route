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

import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.routing.weighting.AbstractAdjustedWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

/**
 * 通过乘以一个系数来修改标记为 access destination 或 private 的边的权重。
 *
 * @author Andrzej Oles
 */
public class LimitedAccessWeighting extends AbstractAdjustedWeighting {
    public static final double MIN_FACTOR = 1;// 确保无需修改 getMinWeight，即 road_access_factor >= 1
    public static final double MAX_FACTOR = 10;
    public static final double DEFAULT_DESTINATION_FACTOR = 1;
    public static final double VEHICLE_DESTINATION_FACTOR = 10;
    public static final double DEFAULT_PRIVATE_FACTOR = 1.2;
    public static final double VEHICLE_PRIVATE_FACTOR = 10;
    public static final double DEFAULT_CUSTOMERS_FACTOR = 1.2;
    public static final double VEHICLE_CUSTOMERS_FACTOR = 1.5;

//    private final EnumEncodedValue<RoadAccess> roadAccessEnc;
    // 该系数对仅允许 "destination" 或 private 通行的道路施加惩罚，参见 GH#733 和 GH#1936
//    private final double destinationPenalty;
//    private final double privatePenalty;
    // 该系数对仅允许 "customers" 通行的道路施加惩罚，参见 ORS#1981
//    private final double customersPenalty;

    public LimitedAccessWeighting(Weighting superWeighting, PMap map) {
        super(superWeighting);

//        destinationPenalty = getFactorValue(encoder, map, "road_access_destination_factor", DEFAULT_DESTINATION_FACTOR, VEHICLE_DESTINATION_FACTOR);
//        privatePenalty = getFactorValue(encoder, map, "road_access_private_factor", DEFAULT_PRIVATE_FACTOR, VEHICLE_PRIVATE_FACTOR);
//        customersPenalty = getFactorValue(encoder, map, "road_access_customers_factor", DEFAULT_CUSTOMERS_FACTOR, VEHICLE_CUSTOMERS_FACTOR);
//
//        roadAccessEnc = destinationPenalty > 1 || privatePenalty > 1 || customersPenalty > 1 ? encoder.getEnumEncodedValue(RoadAccess.KEY, RoadAccess.class) : null;
    }

//    static double getFactorValue(FlagEncoder encoder, PMap map, String key, double defaultFactor, double vehicleFactor) {
//        double defaultValue = encoder.getTransportationMode().isMotorVehicle() ? vehicleFactor : defaultFactor;
//        return checkBounds(key, map.getDouble(key, defaultValue), MIN_FACTOR, MAX_FACTOR);
//    }

    static double checkBounds(String key, double val, double from, double to) {
        if (val < from || val > to)
            throw new IllegalArgumentException(key + " has invalid range should be within [" + from + ", " + to + "]");

        return val;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse, long edgeEnterTime) {
        double weight = super.calcEdgeWeight(edgeState, reverse, edgeEnterTime);

        weight = modifyWeight(weight, edgeState);

        return weight;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        double weight = super.calcEdgeWeight(edgeState, reverse);

        weight = modifyWeight(weight, edgeState);

        return weight;
    }

    private double modifyWeight(double weight, EdgeIteratorState edgeState) {
//        if (roadAccessEnc != null) {
////            RoadAccess access = edgeState.get(roadAccessEnc);
////            if (access == RoadAccess.DESTINATION)
////                weight *= destinationPenalty;
////            else if (access == RoadAccess.PRIVATE)
////                weight *= privatePenalty;
////            else if (access == RoadAccess.CUSTOMERS)
////                weight *= customersPenalty;
////        }
        return weight;
    }

    @Override
    public String toString() {
        return "limited_access|" + this.superWeighting.toString();
    }

    public String getName() {
        return this.superWeighting.getName();
    }
}
