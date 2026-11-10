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
package com.graphhopper.routing.weighting.custom;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.util.SpeedCalculator;
import com.graphhopper.routing.weighting.TurnCostProvider;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;

import static com.graphhopper.routing.weighting.TurnCostProvider.NO_TURN_COST_PROVIDER;

public class CustomWeighting2 implements Weighting {
    public static final String NAME = "custom_two";
    private final double distanceInfluence;
    private final CustomWeighting.EdgeToDoubleMapping edgeToSpeedMapping;
    private final CustomWeighting.EdgeToDoubleMapping edgeToPriorityMapping;
    private final TurnCostProvider turnCostProvider;
    // ORS-GH MOD START - additional field
    protected SpeedCalculator speedCalculator;
    // ORS-GH MOD END

    public CustomWeighting2(EncodedValue encodedValue, TurnCostProvider turnCostProvider, CustomWeighting.Parameters parameters) {
        if (!Weighting.isValidName(getName()))
            throw new IllegalStateException("Not a valid name for a Weighting: " + getName());
        this.turnCostProvider = turnCostProvider;
        this.edgeToSpeedMapping = parameters.getEdgeToSpeedMapping();
        this.edgeToPriorityMapping = parameters.getEdgeToPriorityMapping();
        this.distanceInfluence = parameters.getDistanceInfluence();
    }

    @Override
    public double calcMinWeightPerDistance() {
        return 0.01;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        double priority = edgeToPriorityMapping.get(edgeState, reverse);
        if (priority < 0 || priority > 100)
            throw new IllegalArgumentException("Invalid priority: " + priority + ", must be in [0, 100]");
        else if (priority == 0)
            return Double.POSITIVE_INFINITY;
        return edgeState.getDistance() * (1.0 / priority + distanceInfluence / 1000.0);
    }

    // ORS-GH MOD - additional method
    // needed for time-dependent routing
    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse, long edgeEnterTime) {
        return  calcEdgeWeight(edgeState, reverse, 0);
    }

    @Override
    public long calcEdgeMillis(EdgeIteratorState edgeState, boolean reverse) {
        double speed = edgeToSpeedMapping.get(edgeState, reverse);
        if (speed == 0) return Long.MAX_VALUE;
        return Math.round(edgeState.getDistance() * 1000 / speed * 3.6);
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
        return turnCostProvider != NO_TURN_COST_PROVIDER;
    }

    @Override
    public String getName() {
        return NAME;
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
