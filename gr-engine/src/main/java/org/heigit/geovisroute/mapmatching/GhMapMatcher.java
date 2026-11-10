package org.heigit.geovisroute.mapmatching;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.util.Arrays;
import java.util.List;

import static com.graphhopper.matching.MapMatching.routerFromGraphHopper;

/**
 * GhMapMatcher 是一个门面类，内部使用 GraphHopper 的 MapMatching
 * 实现 ORS 的 AbstractMapMatcher 接口。
 */
public class GhMapMatcher extends AbstractMapMatcher {
    PMap hints;

    public GhMapMatcher(GraphHopper graphHopper, String profile) {
        setGraphHopper(graphHopper);
        hints = new PMap()
                .putObject("profile", profile)
                .putObject(Parameters.Landmark.DISABLE, true);
    }

    @Override
    public RouteSegmentInfo[] match(Coordinate[] locations, boolean bothDirections) {
        MapMatching.Router router = routerFromGraphHopper(graphHopper, hints);
        MapMatching mapMatching = new MapMatching(graphHopper.getBaseGraph(), (LocationIndexTree)graphHopper.getLocationIndex(), router);
        List<Observation> inputGPXEntries = getObservationsFromLocations(locations);
        MatchResult mr = mapMatching.match(inputGPXEntries);
        return getRouteSegmentInfoFromMatchResult(mr);
    }

    private static RouteSegmentInfo[] getRouteSegmentInfoFromMatchResult(MatchResult mr) {
        List<EdgeIteratorState> edgeStates = mr.getMergedPath().calcEdges();
        double distance = mr.getMatchLength();
        long time = mr.getMatchMillis();
        Geometry geometry = null; // TODO: 从匹配结果中获取几何
        // 目前尚不清楚几何是否会被用到，
        // 因此暂时保持原样。
        RouteSegmentInfo[] rsi = new RouteSegmentInfo[1];
        rsi[0] = new RouteSegmentInfo(edgeStates, distance, time, geometry);
        return rsi;
    }

    private static List<Observation> getObservationsFromLocations(Coordinate[] locations) {
        return Arrays.stream(locations).map(
                loc -> new Observation(new GHPoint(loc.y, loc.x))
        ).toList();
    }

    @Override
    public void clear() {

    }
}
