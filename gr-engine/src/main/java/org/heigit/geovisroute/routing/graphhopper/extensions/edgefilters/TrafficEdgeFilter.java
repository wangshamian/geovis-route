package org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters;

import com.graphhopper.routing.querygraph.EdgeIteratorStateHelper;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.routing.graphhopper.extensions.TrafficRelevantWayType;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.TrafficGraphStorage;


public class TrafficEdgeFilter implements EdgeFilter {
    private int hereFunctionalClass;
    private final TrafficGraphStorage trafficGraphStorage;

    public TrafficEdgeFilter(BaseGraph baseGraph) {
        this.trafficGraphStorage = GraphStorageUtils.getGraphExtension(baseGraph, TrafficGraphStorage.class);
    }


    @Override
    public boolean accept(EdgeIteratorState edgeIteratorState) {
        int edgeId = EdgeIteratorStateHelper.getOriginalEdge(edgeIteratorState);
        short osmWayTypeValue = (short) this.trafficGraphStorage.getOrsRoadProperties(edgeId, TrafficGraphStorage.Property.ROAD_TYPE);
        return osmWayTypeValue == hereFunctionalClass;
    }

    public void setHereFunctionalClass(int hereFunctionalClass) {
        this.hereFunctionalClass = hereFunctionalClass;
    }

    public int getHereFunctionalClass() {
        return this.hereFunctionalClass;
    }

    public void lowerFunctionalClass() {
        if (hereFunctionalClass < TrafficRelevantWayType.RelevantWayTypes.CLASS1.value) {
            // 我们不希望将功能等级降低到 4 以下。
            this.hereFunctionalClass += 1;
        } else if (hereFunctionalClass >= TrafficRelevantWayType.RelevantWayTypes.CLASS1LINK.value && hereFunctionalClass < TrafficRelevantWayType.RelevantWayTypes.CLASS4LINK.value) {
            this.hereFunctionalClass += 1;
        }
    }

    public void higherFunctionalClass() {
        if (hereFunctionalClass > TrafficRelevantWayType.RelevantWayTypes.CLASS1.value && hereFunctionalClass <= TrafficRelevantWayType.RelevantWayTypes.CLASS4.value) {
            // 我们不希望将功能等级提高到 CLASS1 以上，也不希望低于 CLASS4，以免与非 link 道路冲突。
            this.hereFunctionalClass -= 1;
        } else if (hereFunctionalClass == TrafficRelevantWayType.RelevantWayTypes.CLASS5.value) {
            // 直接升到 class 4。升级时跳过 unclassified。
            this.hereFunctionalClass -= 2;
        } else if (hereFunctionalClass > TrafficRelevantWayType.RelevantWayTypes.CLASS1LINK.value && hereFunctionalClass <= TrafficRelevantWayType.RelevantWayTypes.CLASS4LINK.value) {
            this.hereFunctionalClass -= 1;
        }
    }
}
