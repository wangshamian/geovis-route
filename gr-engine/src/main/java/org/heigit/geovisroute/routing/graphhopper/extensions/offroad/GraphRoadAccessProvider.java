package org.heigit.geovisroute.routing.graphhopper.extensions.offroad;

import com.graphhopper.GraphHopper;
import com.graphhopper.offroad.RoadAccessProvider;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;

/**
 * 基于 GraphHopper 路网的公路信息提供者：按车辆类型的可通行边查询网格附近的公路及其速度，
 * 供越野混合路由判断路段是公路还是越野。
 */
public class GraphRoadAccessProvider implements RoadAccessProvider {

    private final LocationIndex locationIndex;
    private final EdgeFilter accessFilter;
    private final DecimalEncodedValue speedEncodedValue;
    /**
     * 网格中心到公路边的最大吸附距离（米），超过则认为该网格不在公路上。
     */
    private final double maxSnapDistance;

    public GraphRoadAccessProvider(GraphHopper graphHopper, String vehicle, double maxSnapDistance) {
        this.locationIndex = graphHopper.getLocationIndex();
        EncodingManager encodingManager = graphHopper.getEncodingManager();
        String accessKey = VehicleAccess.key(vehicle);
        String speedKey = VehicleSpeed.key(vehicle);
        if (!encodingManager.hasEncodedValue(accessKey) || !encodingManager.hasEncodedValue(speedKey)) {
            throw new IllegalArgumentException("路网缺少编码值 " + speedKey + "，无法进行公路与越野混合路由");
        }
        this.accessFilter = AccessFilter.allEdges(encodingManager.getBooleanEncodedValue(accessKey));
        this.speedEncodedValue = encodingManager.getDecimalEncodedValue(speedKey);
        this.maxSnapDistance = maxSnapDistance;
    }

    @Override
    public RoadAccess getRoad(double lat, double lon) {
        Snap snap = locationIndex.findClosest(lat, lon, accessFilter);
        if (!snap.isValid() || snap.getQueryDistance() > maxSnapDistance) {
            return null;
        }
        double speedKmh = snap.getClosestEdge().get(speedEncodedValue);
        return speedKmh > 0 ? new RoadAccess(speedKmh) : null;
    }
}
