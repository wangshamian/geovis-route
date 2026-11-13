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
package org.heigit.geovisroute.routing.parameters;

import org.heigit.geovisroute.routing.graphhopper.extensions.VehicleLoadCharacteristicsFlags;

import java.util.List;

public class VehicleParameters extends ProfileParameters {
    private double length = 0.0;
    private double height = 0.0;
    private double width = 0.0;
    private double weight = 0.0;
    private double axleload = 0.0;
    // 军车通过性参数
    private double gradient = 0.0;
    private double fordDepth = 0.0;
    private double bridgeLoad = 0.0;
    private int characteristics = VehicleLoadCharacteristicsFlags.NONE;

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getAxleload() {
        return axleload;
    }

    public void setAxleload(double axleload) {
        this.axleload = axleload;
    }

    public double getGradient() {
        return gradient;
    }

    public void setGradient(double gradient) {
        this.gradient = gradient;
    }

    public double getFordDepth() {
        return fordDepth;
    }

    public void setFordDepth(double fordDepth) {
        this.fordDepth = fordDepth;
    }

    public double getBridgeLoad() {
        return bridgeLoad;
    }

    public void setBridgeLoad(double bridgeLoad) {
        this.bridgeLoad = bridgeLoad;
    }

    public int getLoadCharacteristics() {
        return characteristics;
    }

    public void setLoadCharacteristics(int characteristics) {
        this.characteristics = characteristics;
    }

    public boolean hasAttributes() {
        return height > 0.0 || length > 0.0 || width > 0.0 || weight > 0.0 || axleload > 0.0
                || gradient > 0.0 || fordDepth > 0.0 || bridgeLoad > 0.0 || characteristics != 0;
    }

    @Override
    public List<String> getValidRestrictions() {
        List<String> valid = super.getValidRestrictions();
        valid.add("height");
        valid.add("length");
        valid.add("width");
        valid.add("weight");
        valid.add("hazmat");
        valid.add("axleload");
        valid.add("maximum_gradient");
        valid.add("maximum_ford_depth");
        valid.add("maximum_bridge_load");
        return valid;
    }
}
