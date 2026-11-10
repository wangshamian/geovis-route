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
package org.heigit.geovisroute.routing;

import com.graphhopper.storage.GraphExtension;
import org.heigit.geovisroute.common.DistanceUnit;
import org.heigit.geovisroute.exceptions.StatusCodeException;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.WarningGraphExtension;
import org.heigit.geovisroute.util.DistanceUnitUtil;
import org.heigit.geovisroute.util.FormatUtility;

import java.util.*;

public class RouteExtraInfo {
    private final String name;
    private final List<RouteSegmentItem> segments;
    private double factor = 1.0;
    private boolean usedForWarnings = false;
    private WarningGraphExtension warningGraphExtension;

    public RouteExtraInfo(String name) {
        this(name, null);
    }

    /**
     * 构造函数，可标记该 RouteExtraInfo 是否可以生成警告
     *
     * @param name      附加信息名称
     * @param extension 用于生成附加信息的 GraphExtension。将对其进行检查，
     *                  判断是否为 {@Link org.heigit.geovisroute.routing.graphhopper.extensions.storages.WarningGraphExtension} 类型。
     */
    public RouteExtraInfo(String name, GraphExtension extension) {
        this.name = name;
        segments = new ArrayList<>();
        if (extension instanceof WarningGraphExtension graphExtension) {
            warningGraphExtension = graphExtension;
            usedForWarnings = true;
        }
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public void add(RouteSegmentItem item) {
        segments.add(item);
    }

    public List<RouteSegmentItem> getSegments() {
        return segments;
    }

    public List<ExtraSummaryItem> getSummary(DistanceUnit units, double routeDistance, boolean sort) throws StatusCodeException {
        List<ExtraSummaryItem> summary = new ArrayList<>();

        if (!segments.isEmpty()) {
            Comparator<ExtraSummaryItem> comp = (ExtraSummaryItem a, ExtraSummaryItem b) -> Double.compare(b.getAmount(), a.getAmount());

            double totalDist = 0.0;

            Map<Double, Double> stats = new HashMap<>();

            for (RouteSegmentItem seg : segments) {
                Double scaledValue = seg.getValue() / factor;
                Double value = stats.get(scaledValue);

                if (value == null)
                    stats.put(scaledValue, seg.getDistance());
                else {
                    value += seg.getDistance();
                    stats.put(scaledValue, value);
                }

                totalDist += seg.getDistance();
            }

            if (totalDist != 0.0) {
                int unitDecimals = FormatUtility.getUnitDecimals(units);
                // 某些附加信息（如陡坡度）可能因多次舍入而产生不一致的距离值。
                // 因此尝试对距离进行缩放，使其总和等于路线的总距离
                double distScale = totalDist / routeDistance;

                for (Map.Entry<Double, Double> entry : stats.entrySet()) {
                    double scaledValue = entry.getValue() / distScale;
                    ExtraSummaryItem esi = new ExtraSummaryItem(entry.getKey(),
                            FormatUtility.roundToDecimals(DistanceUnitUtil.convert(scaledValue, DistanceUnit.METERS, units), unitDecimals),
                            FormatUtility.roundToDecimals(scaledValue * 100.0 / routeDistance, 2)
                    );

                    summary.add(esi);
                }

                if (sort)
                    summary.sort(comp);
            }
        }

        return summary;
    }

    public double getFactor() {
        return factor;
    }

    public void setFactor(double factor) {
        this.factor = factor;
    }

    public boolean isUsedForWarnings() {
        return usedForWarnings;
    }

    public WarningGraphExtension getWarningGraphExtension() {
        return warningGraphExtension;
    }
}
