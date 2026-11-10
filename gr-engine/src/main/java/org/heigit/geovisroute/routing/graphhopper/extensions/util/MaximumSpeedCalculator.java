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
package org.heigit.geovisroute.routing.graphhopper.extensions.util;

import com.graphhopper.routing.util.SpeedCalculator;
import com.graphhopper.util.AbstractAdjustedSpeedCalculator;
import com.graphhopper.util.EdgeIteratorState;

/**
 * 速度计算器，在路径计算过程中根据用户设置的最大速度限制速度。
 *
 * @author Andrzej Oles
 */
public class MaximumSpeedCalculator extends AbstractAdjustedSpeedCalculator {
    private final double userMaxSpeed;

    public MaximumSpeedCalculator(SpeedCalculator superSpeedCalculator, double maximumSpeed) {
        super(superSpeedCalculator);

        this.userMaxSpeed = maximumSpeed;
    }

    public double getSpeed(EdgeIteratorState edge, boolean reverse, long time) {
        double speed = superSpeedCalculator.getSpeed(edge, reverse, time);

        if (speed > userMaxSpeed)
            speed = userMaxSpeed;

        return speed;
    }
}
