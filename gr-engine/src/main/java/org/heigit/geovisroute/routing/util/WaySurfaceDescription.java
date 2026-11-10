package org.heigit.geovisroute.routing.util;

import org.heigit.geovisroute.routing.graphhopper.extensions.SurfaceType;

public class WaySurfaceDescription {
    private byte wayType;
    private SurfaceType surfaceType;

    public byte getWayType() {
        return wayType;
    }

    public void setWayType(int wayType) {
        this.wayType = (byte) wayType;
    }

    public SurfaceType getSurfaceType() {
        return surfaceType;
    }

    public void setSurfaceType(SurfaceType surfaceType) {
        this.surfaceType = surfaceType;
    }

    public void reset() {
        wayType = 0;
        surfaceType = SurfaceType.UNKNOWN;
    }
}
