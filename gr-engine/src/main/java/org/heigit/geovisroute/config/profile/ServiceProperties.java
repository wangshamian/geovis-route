package org.heigit.geovisroute.config.profile;


import lombok.Getter;
import lombok.Setter;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class ServiceProperties {
    private Double maximumDistance;
    private Double maximumDistanceDynamicWeights;
    private Double maximumDistanceAvoidAreas;
    private Double maximumDistanceAlternativeRoutes;
    private Double maximumDistanceRoundTripRoutes;
    private Integer maximumWayPoints;
    private Integer maximumSnappingRadius;
    private Integer maximumVisitedNodes;
    private Boolean forceTurnCosts;
    private Boolean allowCustomModels;
    private ExecutionProperties execution = new ExecutionProperties();

    public ServiceProperties() {
    }

    public ServiceProperties(String ignored) {
        // 该构造函数用于创建一个空对象，以便在 JSON 序列化时忽略它。
    }

    public void merge(ServiceProperties other) {
        maximumDistance = ofNullable(this.maximumDistance).orElse(other.maximumDistance);
        maximumDistanceDynamicWeights = ofNullable(this.maximumDistanceDynamicWeights).orElse(other.maximumDistanceDynamicWeights);
        maximumDistanceAvoidAreas = ofNullable(this.maximumDistanceAvoidAreas).orElse(other.maximumDistanceAvoidAreas);
        maximumDistanceAlternativeRoutes = ofNullable(this.maximumDistanceAlternativeRoutes).orElse(other.maximumDistanceAlternativeRoutes);
        maximumDistanceRoundTripRoutes = ofNullable(this.maximumDistanceRoundTripRoutes).orElse(other.maximumDistanceRoundTripRoutes);
        maximumWayPoints = ofNullable(this.maximumWayPoints).orElse(other.maximumWayPoints);
        maximumSnappingRadius = ofNullable(this.maximumSnappingRadius).orElse(other.maximumSnappingRadius);
        maximumVisitedNodes = ofNullable(this.maximumVisitedNodes).orElse(other.maximumVisitedNodes);
        forceTurnCosts = ofNullable(forceTurnCosts).orElse(other.forceTurnCosts);
        allowCustomModels = ofNullable(allowCustomModels).orElse(other.allowCustomModels);
        execution.merge(other.execution);
    }
}
