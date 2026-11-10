package org.heigit.geovisroute.routing.traffic;

import com.graphhopper.routing.querygraph.VirtualEdgeIteratorState;
import com.graphhopper.routing.util.SpeedCalculator;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.AbstractAdjustedSpeedCalculator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.TrafficGraphStorage;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TrafficSpeedCalculator extends AbstractAdjustedSpeedCalculator {
    // 时间相关的内容
    protected TrafficGraphStorage trafficGraphStorage;
    protected int timeZoneOffset;
    private boolean isVehicle = false;
    private boolean isHGV = false;
    private final double HGVTrafficSpeedLimit = 80.0;

    public TrafficSpeedCalculator(SpeedCalculator superSpeedCalculator) {
        super(superSpeedCalculator);
    }

    public void init(BaseGraph graph, String encodeName) {
//        if (flagEncoder instanceof VehicleFlagEncoder vehicleFE)
//            setVehicleFlagEncoder(vehicleFE);
//        if (flagEncoder instanceof HeavyVehicleFlagEncoder)
//            isHGV = true;
        setTrafficGraphStorage(GraphStorageUtils.getGraphExtension(graph, TrafficGraphStorage.class));
    }

    @Override
    public double getSpeed(EdgeIteratorState edge, boolean reverse, long time) {
        double speed = superSpeedCalculator.getSpeed(edge, reverse, time);

        int edgeKey = getEdgeKey(edge, reverse);

        double trafficSpeed;
        if (time == -1)
            trafficSpeed = trafficGraphStorage.getMaxSpeedValue(edgeKey);
        else
            trafficSpeed = trafficGraphStorage.getSpeedValue(edgeKey, time, timeZoneOffset);

        if (trafficSpeed > 0) {
            //TODO: 这是一种启发式方法，用于在给定交通数据和 ORS 内部速度计算的情况下给出预期结果。
//            if (isVehicle) {
//                trafficSpeed = vehicleFlagEncoder.adjustSpeedForAcceleration(edge.getDistance(), trafficSpeed);
//                // For heavy vehicles, consider the traffic speeds only up to a predefined speeds
//                if (!isHGV || (isHGV && trafficSpeed <= HGVTrafficSpeedLimit)) {
//                    speed = trafficSpeed;
//                }
//            } else {
                if (speed >= 45.0 && !(trafficSpeed > 1.1 * speed) || trafficSpeed < speed) {
                    speed = trafficSpeed;
//                }
            }
        }

        return speed;
    }

    protected int getEdgeKey(EdgeIteratorState edge, boolean reverse) {
        int edgeKey;
        if (edge instanceof VirtualEdgeIteratorState iteratorState) {
            edgeKey = iteratorState.getOriginalEdgeKey();
        } else {
            edgeKey = edge.getEdgeKey();
        }
        if (reverse)
            edgeKey = GHUtility.reverseEdgeKey(edgeKey);
        return edgeKey;
    }

    public void setTrafficGraphStorage(TrafficGraphStorage trafficGraphStorage) {
        this.trafficGraphStorage = trafficGraphStorage;
    }

    public void setZonedDateTime(ZonedDateTime zdt) {
        this.timeZoneOffset = zdt.getOffset().getTotalSeconds() / 3600;
    }

    public ZoneId getZoneId() {
        return trafficGraphStorage.getZoneId();
    }

    @Override
    public boolean isTimeDependent() {
        return true;
    }
}
