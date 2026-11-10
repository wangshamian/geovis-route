/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library;
 *  if not, see <https://www.gnu.org/licenses/>.
 */
package org.heigit.geovisroute.routing.graphhopper.extensions;

import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.PMap;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.routing.RouteSearchParameters;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;
import org.heigit.geovisroute.routing.parameters.VehicleParameters;
import org.heigit.geovisroute.routing.parameters.WheelchairParameters;
import org.locationtech.jts.geom.Polygon;

public class GREdgeFilterFactory implements EdgeFilterFactory {
    private static final Logger LOGGER = Logger.getLogger(GREdgeFilterFactory.class.getName());

    public EdgeFilter createEdgeFilter(PMap opts, String encodeName, BaseGraph baseGraph, EncodingManager encodingManager) {
        return createEdgeFilter(opts, encodeName, baseGraph, null, encodingManager);
    }

    public EdgeFilter createEdgeFilter(PMap opts, String encodeName, BaseGraph baseGraph, EdgeFilter prependFilter,
                                       EncodingManager encodingManager) {
        /* 初始化空的边过滤器序列 */
        EdgeFilterSequence edgeFilters = new EdgeFilterSequence();

        if (prependFilter != null)
            edgeFilters.add(prependFilter);

        /* 默认边过滤器，接受指定车辆的两个方向。GraphHopper 11 将车辆
         * 通行权限保留在图自身的 EncodingManager 中，临时的 SimpleBooleanEncodedValue 没有位位置，
         * 因此会拒绝所有边。 */
        String ghVehicle = FlagEncoderNames.getGHVehicleName(encodeName);
        edgeFilters.add(AccessFilter.allEdges(encodingManager.getBooleanEncodedValue(VehicleAccess.key(ghVehicle))));
        try {
            if (opts == null) {
                opts = new PMap();
            }

            /* 避让区域 */
            if (opts.has("avoid_areas")) {
                edgeFilters.add(new AvoidAreasEdgeFilter(opts.getObject("avoid_areas", new Polygon[]{})));
            }

            /* 重型车辆过滤器 */
            if (opts.has("edgefilter_hgv")) {
                edgeFilters.add(new HeavyVehicleEdgeFilter(opts.getInt("edgefilter_hgv", 0), opts.getObject("routing_profile_params", new VehicleParameters()), baseGraph));
            }

            /* 轮椅过滤器 */
            else if (opts.has("edgefilter_wheelchair")) {
                edgeFilters.add(new WheelchairEdgeFilter(opts.getObject("routing_profile_params", new WheelchairParameters()), baseGraph));
            }

            /* 避让要素 */
            if (opts.has("avoid_features") && opts.has("routing_profile_type")) {
                edgeFilters.add(new AvoidFeaturesEdgeFilter(opts.getInt("routing_profile_type", 0), opts.getObject("avoid_features", new RouteSearchParameters()), baseGraph));
            }

            /* 避让国界 */
            if (opts.has("avoid_borders")) {
                edgeFilters.add(new AvoidBordersEdgeFilter(opts.getObject("avoid_borders", new RouteSearchParameters()), baseGraph));
            }

        } catch (Exception ex) {
            LOGGER.error(ex);
        }
        return edgeFilters;
    }
}
