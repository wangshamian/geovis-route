/*
 *
 *  *
 *  *  *  Licensed to GIScience Research Group, Heidelberg University (GIScience)
 *  *  *
 *  *  *   http://www.giscience.uni-hd.de
 *  *  *   http://www.heigit.org
 *  *  *
 *  *  *  under one or more contributor license agreements. See the NOTICE file
 *  *  *  distributed with this work for additional information regarding copyright
 *  *  *  ownership. The GIScience licenses this file to you under the Apache License,
 *  *  *  Version 2.0 (the "License"); you may not use this file except in compliance
 *  *  *  with the License. You may obtain a copy of the License at
 *  *  *
 *  *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  *  Unless required by applicable law or agreed to in writing, software
 *  *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  See the License for the specific language governing permissions and
 *  *  *  limitations under the License.
 *  *
 *
 */

package org.heigit.geovisroute.util.mockuputil;

import org.heigit.geovisroute.common.DistanceUnit;
import org.heigit.geovisroute.routing.RouteInstructionsFormat;
import org.heigit.geovisroute.routing.RoutingRequest;
import org.heigit.geovisroute.routing.pathprocessors.BordersExtractor;
import org.locationtech.jts.geom.Coordinate;

/**
 * 这是一个 {@link org.heigit.geovisroute.routing.RoutingRequest} 模拟类，用于 junit 测试及其他需要的地方。
 * 该模拟类尽可能保持通用且非常灵活。集成前请务必检查并理解其中的元素！
 * routeProfile 枚举作为变量，使新建个性化模拟对象变得简单灵活。
 * 新增模拟配置时，先将其加入枚举，再加入 create 函数中作为新的 "else if()" 分支。
 * 调用示例："RoutingRequest routingRequestMockup = new RoutingRequestMockup().create(RoutingRequestMockup.routeProfile.standard2d)"
 *
 * @author Julian Psotta, julian@openrouteservice.org
 */
// TODO 重构：检查为何此处未被使用却仍然保留
public class RoutingRequestMockup {

    private final RoutingRequest routingRequest;


    // 目前仅集成了两个配置，二者都对应 Heidelberg 的图！
    public enum routeProfile {
        STANDARD_HEIDELBERG_2D, STANDARD_HEIDELBERG_3D
    }

    private enum searchParamProfile {
        STANDARD_CAR_SEARCH_PROFILE
    }

    private final Coordinate[] coords2d = new Coordinate[3];
    private final Coordinate[] coords3d = new Coordinate[3];

    public RoutingRequestMockup() {
        routingRequest = new RoutingRequest();
    }

    /**
     * 该函数的作用是使用标准化变量完整初始化 RouteRequestMockup。
     * 坐标以 Heidelberg 为参考，且为二维坐标！！！因此如果图中不包含 Heidelberg，需要调整坐标。
     * 思路：在某一处统一管理坐标的选择。
     *
     * @return @{@link RoutingRequest} 用于模拟测试。
     */
    public RoutingRequest create(routeProfile profile) throws Exception {

        if (profile == RoutingRequestMockup.routeProfile.STANDARD_HEIDELBERG_2D) {
            routingRequest.setGeometryFormat("geojson");
            // 填充二维坐标
            coords2d[0] = new Coordinate(Double.parseDouble("8.690614"), Double.parseDouble("49.38365"));
            coords2d[1] = new Coordinate(Double.parseDouble("8.7007"), Double.parseDouble("49.411699"));
            coords2d[2] = new Coordinate(Double.parseDouble("8.7107"), Double.parseDouble("49.4516"));
            routingRequest.setCoordinates(coords2d);
            routingRequest.setIncludeElevation(false);
            routingRequest.setIncludeGeometry(true);
            routingRequest.setAttributes(null);
            routingRequest.setContinueStraight(false);
            routingRequest.setExtraInfo(0);
            routingRequest.setIncludeInstructions(true);
            routingRequest.setIncludeManeuvers(false);
            routingRequest.setIncludeRoundaboutExits(false);
            routingRequest.setInstructionsFormat(RouteInstructionsFormat.TEXT);
            routingRequest.setLanguage("DE");
            routingRequest.setLocationIndex(-1);
            routingRequest.setUnits(DistanceUnit.METERS);
            // 搜索参数只能通过本地访问方式设置。
            setRouteSearchParameters(searchParamProfile.STANDARD_CAR_SEARCH_PROFILE);
            routingRequest.setId(null);
            return routingRequest;
        } else if (profile == RoutingRequestMockup.routeProfile.STANDARD_HEIDELBERG_3D) {
            routingRequest.setGeometryFormat("geojson");
            // 填充三维坐标
            // TODO 填入第三个坐标！！！
            coords3d[0] = new Coordinate(Double.parseDouble("8.690614"), Double.parseDouble("49.38365"), Double.parseDouble("NaN"));
            coords3d[1] = new Coordinate(Double.parseDouble("8.7007"), Double.parseDouble("49.411699"), Double.parseDouble("NaN"));
            coords3d[2] = new Coordinate(Double.parseDouble("8.7107"), Double.parseDouble("49.4516"), Double.parseDouble("NaN"));
            routingRequest.setCoordinates(coords3d);
            routingRequest.setIncludeElevation(false);
            routingRequest.setIncludeGeometry(true);
            routingRequest.setAttributes(null);
            routingRequest.setContinueStraight(false);
            routingRequest.setExtraInfo(0);
            routingRequest.setIncludeInstructions(true);
            routingRequest.setIncludeManeuvers(false);
            routingRequest.setIncludeRoundaboutExits(false);
            routingRequest.setInstructionsFormat(RouteInstructionsFormat.TEXT);
            routingRequest.setLanguage("DE");
            routingRequest.setLocationIndex(-1);
            routingRequest.setUnits(DistanceUnit.METERS);
            // 搜索参数只能通过本地访问方式设置。
            setRouteSearchParameters(searchParamProfile.STANDARD_CAR_SEARCH_PROFILE);
            routingRequest.setId(null);
            return routingRequest;
        }
        return null;

    }

    /**
     * 该函数使用自定义配置中的标准化变量，从 {@link RoutingRequest} 初始化 {@link org.heigit.geovisroute.routing.RouteSearchParameters}。
     */
    private void setRouteSearchParameters(searchParamProfile profile) throws Exception {
        if (profile == searchParamProfile.STANDARD_CAR_SEARCH_PROFILE) {
            routingRequest.getSearchParameters().setAvoidAreas(null);
            routingRequest.getSearchParameters().setAvoidBorders(BordersExtractor.Avoid.NONE);
            routingRequest.getSearchParameters().setAvoidCountries(null);
            routingRequest.getSearchParameters().setAvoidFeatureTypes(0);
            routingRequest.getSearchParameters().setBearings(null);
            routingRequest.getSearchParameters().setConsiderTurnRestrictions(false);
            routingRequest.getSearchParameters().setFlexibleMode(false);
            routingRequest.getSearchParameters().setMaximumRadiuses(null);
            routingRequest.getSearchParameters().setProfileType(1);
            routingRequest.getSearchParameters().setVehicleType(0);
            routingRequest.getSearchParameters().setWeightingMethod(1);
        }
    }
}