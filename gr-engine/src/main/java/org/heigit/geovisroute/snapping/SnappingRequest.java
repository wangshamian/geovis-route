package org.heigit.geovisroute.snapping;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.common.ServiceRequest;
import org.heigit.geovisroute.matrix.MatrixSearchContext;
import org.heigit.geovisroute.matrix.MatrixSearchContextBuilder;
import org.heigit.geovisroute.routing.RoutingProfile;
import org.heigit.geovisroute.routing.RoutingProfileType;
import org.heigit.geovisroute.routing.WeightingMethod;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRWeightingFactory;
import org.heigit.geovisroute.util.ProfileTools;
import org.locationtech.jts.geom.Coordinate;

public class SnappingRequest extends ServiceRequest {
    private String profileName;
    private final int profileType;
    private final Coordinate[] locations;
    private final double maximumSearchRadius;
    private int maximumLocations;

    public SnappingRequest(int profileType, Coordinate[] locations, double maximumSearchRadius) {
        this.profileType = profileType;
        this.locations = locations;
        this.maximumSearchRadius = maximumSearchRadius;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public int getProfileType() {
        return profileType;
    }

    public Coordinate[] getLocations() {
        return locations;
    }

    public double getMaximumSearchRadius() {
        return maximumSearchRadius;
    }

    public void setMaximumLocations(int maximumLocations) {
        this.maximumLocations = maximumLocations;
    }

    public int getMaximumLocations() {
        return maximumLocations;
    }

    public SnappingResult computeResult(RoutingProfile rp) throws Exception {
        GraphHopper gh = rp.getGraphhopper();
        String encoderName = RoutingProfileType.getEncoderName(getProfileType());
        EncodingManager encodingManager = gh.getEncodingManager();
        BooleanEncodedValue booleanEncodedValue = (BooleanEncodedValue) encodingManager.getEncodedValue(encoderName, EncodedValue.class);
        PMap hintsMap = new PMap();
        int weightingMethod = WeightingMethod.RECOMMENDED; // 仅用于创建 profile 字符串
        ProfileTools.setWeightingMethod(hintsMap, weightingMethod, getProfileType(), false);
        ProfileTools.setWeighting(hintsMap, weightingMethod, getProfileType(), false);
        String localProfileName = ProfileTools.makeProfileName(encoderName, hintsMap.getString("weighting", ""), false);
        BaseGraph baseGraph = gh.getBaseGraph();
        String graphDate = gh.getProperties().get("datareader.import.date");

        // TODO: 用 snapping 专用类替换对 matrix search context 的使用

        MatrixSearchContextBuilder builder = new MatrixSearchContextBuilder(baseGraph, gh.getEncodingManager(), gh.getLocationIndex(), AccessFilter.allEdges(booleanEncodedValue), true);
        Weighting weighting = new GRWeightingFactory(baseGraph, gh.getEncodingManager()).createWeighting(gh.getProfile(localProfileName), hintsMap, false);
        MatrixSearchContext mtxSearchCntx = builder.create(baseGraph.getBaseGraph(), null, weighting, localProfileName, getLocations(), getLocations(), getMaximumSearchRadius());
        return new SnappingResult(mtxSearchCntx.getSources().getLocations(), graphDate);
    }
}
