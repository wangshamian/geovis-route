/*
 * This file is part of Openrouteservice.
 *
 * Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, see <https://www.gnu.org/licenses/>.
 */

package org.heigit.geovisroute.api.responses.routing.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.routing.ExtraSummaryItem;
import org.heigit.geovisroute.routing.RouteSegmentItem;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "JSONExtra", description = "表示所请求的某个附加信息项的对象")
public class JSONExtra {
    private final List<List<Long>> values;
    private final List<JSONExtraSummary> summary;

    public JSONExtra(List<RouteSegmentItem> segments, List<ExtraSummaryItem> summaryItems) {
        values = new ArrayList<>();

        for (RouteSegmentItem item : segments) {
            List<Long> segment = new ArrayList<>();
            segment.add((long) item.getFrom());
            segment.add((long) item.getTo());
            segment.add(item.getValue());
            values.add(segment);
        }

        summary = new ArrayList<>();
        for (ExtraSummaryItem item : summaryItems) {
            summary.add(new JSONExtraSummary(item.getValue(), item.getDistance(), item.getAmount()));
        }
    }

    @Schema(description = """
            表示路线某一段的数值列表。各数值含义如下：\s
            数值 1：该段几何起点的索引，
            数值 2：该段几何终点的索引，
            数值 3：分配给该段的 [Value](https://GIScience.github.io/openrouteservice/api-reference/endpoints/directions/extra-info/)。""",
            example = "[[0,3,26],[3,10,12]]")
    @JsonProperty("values")
    private List<List<Long>> getValues() {
        return values;
    }

    @Schema(description = "表示附加信息项汇总的列表。")
    @JsonProperty("summary")
    private List<JSONExtraSummary> getSummary() {
        return summary;
    }
}
