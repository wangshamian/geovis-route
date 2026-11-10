package com.graphhopper.routing.util;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser;
import com.graphhopper.util.EdgeIteratorState;

/**
 * Retrieve default speed
 *
 * @author Andrzej Oles
 */
public class DefaultSpeedCalculator implements SpeedCalculator{
    protected final DecimalEncodedValue avSpeedEnc;

    public DefaultSpeedCalculator(EncodedValue encodedValue) {
        avSpeedEnc = (DecimalEncodedValue) encodedValue;
    }

    @Override
    public double getSpeed(EdgeIteratorState edge, boolean reverse, long time) {
        return reverse ? edge.getReverse(avSpeedEnc) : edge.get(avSpeedEnc);
    }

    @Override
    public boolean isTimeDependent() {
        return false;
    }
}
