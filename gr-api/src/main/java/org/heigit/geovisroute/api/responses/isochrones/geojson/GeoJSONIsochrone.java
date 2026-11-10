package org.heigit.geovisroute.api.responses.isochrones.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.heigit.geovisroute.isochrones.Isochrone;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class GeoJSONIsochrone extends GeoJSONIsochroneBase {
    private final Isochrone isochrone;

    @JsonProperty("properties")
    public GeoJSONIsochroneProperties properties;

    public GeoJSONIsochrone(Isochrone isochrone, Coordinate center, int travellerId) {
        this.isochrone = isochrone;
        properties = new GeoJSONIsochroneProperties(this.isochrone, center, travellerId);
    }

    @Override
    Geometry getIsochroneGeometry() {
        return isochrone.getGeometry();
    }
}
