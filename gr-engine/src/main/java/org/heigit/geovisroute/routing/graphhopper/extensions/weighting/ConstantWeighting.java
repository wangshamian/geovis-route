package org.heigit.geovisroute.routing.graphhopper.extensions.weighting;

import com.graphhopper.routing.util.SpeedCalculator;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeIteratorState;

public class ConstantWeighting implements Weighting {
    private final double weight;
    private final long millis;

    public ConstantWeighting(double weight, long millis) {
        this.weight = weight;
        this.millis = millis;
    }

    @Override
    public double calcMinWeightPerDistance() {
        return 0.01;
    }

    public double getMinWeight(double distance) {
        return weight;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeIteratorState, boolean reverse) {
        return weight;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeIteratorState, boolean reverse, long edgeEnterTime) {
        return calcEdgeWeight(edgeIteratorState, reverse);
    }

    @Override
    public long calcEdgeMillis(EdgeIteratorState edgeIteratorState, boolean reverse) {
        return millis;
    }

    @Override
    public long calcEdgeMillis(EdgeIteratorState edgeIteratorState, boolean reverse, long edgeEnterTime) {
        return calcEdgeMillis(edgeIteratorState, reverse);
    }

    @Override
    public double calcTurnWeight(int inEdge, int viaNode, int outEdge) {
        return weight;
    }

    @Override
    public long calcTurnMillis(int inEdge, int viaNode, int outEdge) {
        return millis;
    }

    @Override
    public boolean hasTurnCosts() {
        return false;
    }

    @Override
    public String getName() {
        return "constant(" + weight + ")";
    }

    @Override
    public boolean isTimeDependent() {
        return false;
    }

    @Override
    public SpeedCalculator getSpeedCalculator() {
        return null;
    }

    @Override
    public void setSpeedCalculator(SpeedCalculator speedCalculator) {
        throw new UnsupportedOperationException();
    }
}
