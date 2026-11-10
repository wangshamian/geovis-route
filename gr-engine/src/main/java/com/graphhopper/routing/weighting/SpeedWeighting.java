/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.routing.weighting;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.SpeedCalculator;
import com.graphhopper.storage.TurnCostStorage;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

public class SpeedWeighting implements Weighting {
    protected final DecimalEncodedValue speedEnc;
    protected final TurnCostProvider turnCostProvider;
    // ORS-GH MOD START - additional field
    protected SpeedCalculator speedCalculator;
    // ORS-GH MOD END

    public SpeedWeighting(DecimalEncodedValue speedEnc) {
        this(speedEnc, TurnCostProvider.NO_TURN_COST_PROVIDER);
    }

    public SpeedWeighting(DecimalEncodedValue speedEnc, DecimalEncodedValue turnCostEnc, TurnCostStorage turnCostStorage, double uTurnCosts) {
        if (turnCostStorage == null || turnCostEnc == null)
            throw new IllegalArgumentException("This SpeedWeighting constructor expects turnCostEnc and turnCostStorage to be != null");
        if (uTurnCosts < 0) throw new IllegalArgumentException("u-turn costs must be positive");
        this.speedEnc = speedEnc;
        this.turnCostProvider = new TurnCostProvider() {
            @Override
            public double calcTurnWeight(int inEdge, int viaNode, int outEdge) {
                if (!EdgeIterator.Edge.isValid(inEdge) || !EdgeIterator.Edge.isValid(outEdge))
                    return 0;
                if (inEdge == outEdge)
                    return Math.max(turnCostStorage.get(turnCostEnc, inEdge, viaNode, outEdge), uTurnCosts);
                else
                    return turnCostStorage.get(turnCostEnc, inEdge, viaNode, outEdge);
            }

            @Override
            public long calcTurnMillis(int inEdge, int viaNode, int outEdge) {
                return (long) (1000 * calcTurnWeight(inEdge, viaNode, outEdge));
            }
        };
    }

    public SpeedWeighting(DecimalEncodedValue speedEnc, TurnCostProvider turnCostProvider) {
        this.speedEnc = speedEnc;
        this.turnCostProvider = turnCostProvider;
    }

    @Override
    public double calcMinWeightPerDistance() {
        return 1 / speedEnc.getMaxStorableDecimal();
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        double speed = reverse ? edgeState.getReverse(speedEnc) : edgeState.get(speedEnc);
        if (speed == 0) return Double.POSITIVE_INFINITY;
        return edgeState.getDistance() / speed;
    }

    // ORS-GH MOD - additional method
    // needed for time-dependent routing
    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse, long edgeEnterTime) {
        return  calcEdgeWeight(edgeState, reverse, 0);
    }

    @Override
    public long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse) {
        return (long) (1000 * calcEdgeWeight(edgeState, reverse));
    }

    // GH orig: public long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse) {
    public long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse, long edgeEnterTime) {
        // ORS-GH MOD END
        // special case for loop edges: since they do not have a meaningful direction we always need to read them in
        // forward direction
        if (edgeState.getBaseNode() == edgeState.getAdjNode()) {
            reverse = false;
        }

        // ORS-GH MOD START
        //double speed = reverse ? edgeState.getReverse(avSpeedEnc) : edgeState.get(avSpeedEnc);
        double speed = speedCalculator.getSpeed(edgeState, reverse, edgeEnterTime);
        // ORS-GH MOD END
        if (Double.isInfinite(speed) || Double.isNaN(speed) || speed < 0)
            throw new IllegalStateException("Invalid speed stored in edge! " + speed);
        if (speed == 0)
            throw new IllegalStateException("Speed cannot be 0 for unblocked edge, use access properties to mark edge blocked! Should only occur for shortest path calculation. See #242.");

        return (long) (edgeState.getDistance() * 3600 / speed);
    }

    @Override
    public double calcTurnWeight(int inEdge, int viaNode, int outEdge) {
        return turnCostProvider.calcTurnWeight(inEdge, viaNode, outEdge);
    }

    @Override
    public long calcTurnMillis(int inEdge, int viaNode, int outEdge) {
        return turnCostProvider.calcTurnMillis(inEdge, viaNode, outEdge);
    }

    @Override
    public boolean hasTurnCosts() {
        return turnCostProvider != TurnCostProvider.NO_TURN_COST_PROVIDER;
    }

    @Override
    public String getName() {
        return "speed";
    }

    // ORS-GH MOD START - additional methods
    @Override
    public boolean isTimeDependent() {
        return false;
    }

    @Override
    public SpeedCalculator getSpeedCalculator() {
        return speedCalculator;
    }

    @Override
    public void setSpeedCalculator(SpeedCalculator speedCalculator) {
        this.speedCalculator = speedCalculator;
    }
    // ORS-GH MOD END
}
