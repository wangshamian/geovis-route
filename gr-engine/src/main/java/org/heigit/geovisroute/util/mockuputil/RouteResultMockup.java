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

import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import org.heigit.geovisroute.common.DistanceUnit;
import org.heigit.geovisroute.routing.RouteResult;
import org.heigit.geovisroute.routing.RouteSegment;
import org.heigit.geovisroute.routing.RouteStep;

import java.math.BigDecimal;

/**
 * 这是一个 {@link org.heigit.geovisroute.routing.RouteResult} 模拟类，用于 junit 测试及其他需要的地方。
 * 该模拟类尽可能保持通用。集成前请务必通过调试检查其中的元素。
 * 目前该类仅支持单条路线！
 * <p>
 * !!!实际上没有其他方式可以高效地创建 {@link RouteResult} 模拟对象!!!
 *
 * @author Julian Psotta, julian@openrouteservice.org
 */
public class RouteResultMockup {

    // 该枚举保存可用的配置
    public enum routeResultProfile {
        STANDARD_HEIDELBERG
    }


    /**
     * 该函数使用标准化值创建 {@link RouteResult} 模拟对象。
     * 可按需添加配置。
     *
     * @param profile 输入必须为 routeResultProfile.* 中的枚举值，例如 routeResultProfile.standardHeidelberg
     * @return 返回值是与配置对应的标准化 {@link RouteResult}。
     * @throws Exception 若发生错误则抛出异常。
     */
    public static RouteResult[] create(routeResultProfile profile) throws Exception {
        RouteResult routeResult = new RouteResult(0);
        if (profile == routeResultProfile.STANDARD_HEIDELBERG) {
            BBox bbox = new BBox(8.690603626917166, 8.690675487235653, 49.38366164068056, 49.38376283758349);
            routeResult.getSummary().setAscent(0.0);
            routeResult.getSummary().setAverageSpeed(0.0);
            routeResult.getSummary().setBBox(bbox);
            routeResult.getSummary().setDescent(0.0);
            routeResult.getSummary().setDistance(0.0);
            routeResult.getSummary().setDuration(0.0);
            PointList pointList = new PointList();
            pointList.add(8.690675487235653, 49.38366164068056);
            pointList.add(8.69063028001704, 49.38376283758349);
            pointList.add(8.690444946820548, 49.38375538700272);
            pointList.add(8.6904630144789, 49.384111711027735);
            pointList.add(8.690476425524274, 49.38459562624832);
            pointList.add(8.690506107646703, 49.38539990448134);
            routeResult.addPointsToGeometry(pointList, false, false);
            ResponsePath responsePath = new ResponsePath();
            responsePath.setDistance(0.0);
            responsePath.setInstructions(null);
            responsePath.setTime(0);
            responsePath.setAscend(0.0);
            responsePath.setDescend(0.0);
            responsePath.setDescription(null);
            responsePath.setFare(BigDecimal.ZERO);
            responsePath.setNumChanges(2);
            responsePath.setRouteWeight(0.0);
            responsePath.setPoints(pointList);
            RouteSegment routeSegment = new RouteSegment(responsePath, DistanceUnit.METERS);
            // 创建第一个 routeStep
            RouteStep routeStep1 = new RouteStep();
            routeStep1.setDistance(0.0);
            routeStep1.setDuration(0.0);
            routeStep1.setMessage(null);
            routeStep1.setMessageType(-1);
            routeStep1.setInstruction("");
            routeStep1.setName("");
            routeStep1.setExitNumber(-1);
            routeStep1.setType(11);
            routeStep1.setWayPoints(new int[]{0, 1});
            routeStep1.setManeuver(null);
            routeStep1.setRoundaboutExitBearings(null);
            routeSegment.addStep(routeStep1);

            // 创建第二个 routeStep
            RouteStep routeStep2 = new RouteStep();
            routeStep2.setDistance(0.0);
            routeStep2.setDuration(0.0);
            routeStep2.setMessage(null);
            routeStep2.setMessageType(-1);
            routeStep2.setInstruction("");
            routeStep2.setName("");
            routeStep2.setExitNumber(-1);
            routeStep2.setType(0);
            routeStep2.setWayPoints(new int[]{1, 2});
            routeStep2.setManeuver(null);
            routeStep2.setRoundaboutExitBearings(null);
            routeSegment.addStep(routeStep2);

            // 创建第三个 routeStep
            RouteStep routeStep3 = new RouteStep();
            routeStep3.setDistance(0.0);
            routeStep3.setDuration(0.0);
            routeStep3.setMessage(null);
            routeStep3.setMessageType(-1);
            routeStep3.setInstruction("");
            routeStep3.setName("");
            routeStep3.setExitNumber(-1);
            routeStep3.setType(10);
            routeStep3.setWayPoints(new int[]{2, 2});
            routeStep3.setManeuver(null);
            routeStep3.setRoundaboutExitBearings(null);
            routeSegment.addStep(routeStep3);
            // 添加 RouteSegment
            routeResult.addSegment(routeSegment);
            routeResult.addWayPointIndex(0);
            routeResult.addWayPointIndex(2);

            return new RouteResult[]{routeResult};
        } else {
            return new RouteResult[]{};
        }
    }
}
