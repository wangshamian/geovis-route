package org.heigit.geovisroute.api.requests.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.graphhopper.jackson.StatementDeserializer;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.JsonFeatureCollection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteRequestCustomModel {
    @JsonProperty("distance_influence")
    @Schema(description = "用于确定两点间距离对边权重影响的参数")
    private double distanceInfluence;

    @JsonProperty("heading_penalty")
    @Schema(hidden = true)
    private double headingPenalty = (double) 300.0F;

    @JsonProperty("speed")
    @JsonDeserialize(contentUsing = StatementDeserializer.class)
    @Schema(description = "描述应用于边速度的规则的对象数组")
    private List<Statement> speedStatements = new ArrayList<>();

    @JsonProperty("priority")
    @JsonDeserialize(contentUsing = StatementDeserializer.class)
    @Schema(description = "描述应用于边优先级的规则的对象数组")
    private List<Statement> priorityStatements = new ArrayList<>();

    @JsonProperty("areas")
    @Schema(implementation = RouteRequestCustomModelAreas.class, description = "可在速度规则和优先级规则中引用的区域映射")
    private JsonFeatureCollection areas = new JsonFeatureCollection();

    public CustomModel toGHCustomModel() {
        CustomModel customModel = new CustomModel();
        customModel.setDistanceInfluence(this.distanceInfluence);
        customModel.setHeadingPenalty(this.headingPenalty);
        this.speedStatements.forEach(customModel::addToSpeed);
        this.priorityStatements.forEach(customModel::addToPriority);
        customModel.setAreas(this.areas);
        return customModel;
    }
}
