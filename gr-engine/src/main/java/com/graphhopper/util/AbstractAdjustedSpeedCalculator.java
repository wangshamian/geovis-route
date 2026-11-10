package com.graphhopper.util;

import com.graphhopper.routing.util.SpeedCalculator;

/**
 * Retrieve default speed
 *
 * @author Andrzej Oles
 */
// ORS-GH MOD START - additional class
public abstract class AbstractAdjustedSpeedCalculator implements SpeedCalculator {
    protected final SpeedCalculator superSpeedCalculator;

    public AbstractAdjustedSpeedCalculator(SpeedCalculator superSpeedCalculator) {
        if (superSpeedCalculator == null)
            throw new IllegalArgumentException("No super calculator set");
        this.superSpeedCalculator = superSpeedCalculator;
    }

    @Override
    public boolean isTimeDependent() {
        return superSpeedCalculator.isTimeDependent();
    }
}
// ORS-GH MOD END
