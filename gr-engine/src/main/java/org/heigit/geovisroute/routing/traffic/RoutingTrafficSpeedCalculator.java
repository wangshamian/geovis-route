package org.heigit.geovisroute.routing.traffic;

import com.graphhopper.routing.util.SpeedCalculator;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;

public class RoutingTrafficSpeedCalculator extends TrafficSpeedCalculator {
    private final double maxPossibleSpeed;

    public RoutingTrafficSpeedCalculator(SpeedCalculator superSpeedCalculator, BaseGraph baseGraph, String encodeName) {
        super(superSpeedCalculator);
        init(baseGraph, encodeName);
        maxPossibleSpeed = 140;
    }

    @Override
    public double getSpeed(EdgeIteratorState edge, boolean reverse, long time) {
        double speed = super.getSpeed(edge, reverse, time);

        if (speed > maxPossibleSpeed)
            speed = maxPossibleSpeed;

        return speed;
    }
}
