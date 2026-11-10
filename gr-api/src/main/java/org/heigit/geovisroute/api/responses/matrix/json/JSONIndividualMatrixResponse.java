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

package org.heigit.geovisroute.api.responses.matrix.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.requests.matrix.MatrixRequest;
import org.heigit.geovisroute.matrix.MatrixMetricsType;
import org.heigit.geovisroute.matrix.MatrixResult;
import org.heigit.geovisroute.util.FormatUtility;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(name = "JSONIndividualMatrixResponse", description = "由服务创建的单个基于 JSON 的路线")
public class JSONIndividualMatrixResponse extends JSONBasedIndividualMatrixResponse {
    @Schema(description = "矩阵计算得到的时长。", example = "[[0,25],[25,0]]")
    @JsonProperty("durations")
    private Double[][] durations;

    @Schema(description = "矩阵计算得到的距离。", example = "[[0,0.25],[0.25,0]]")
    @JsonProperty("distances")
    private Double[][] distances;

    @Schema(description = "矩阵计算中的各个目的地。")
    @JsonProperty("destinations")
    private List<JSON2DDestinations> destinations;

    @Schema(description = "矩阵计算中的各个起点。")
    @JsonProperty("sources")
    private List<JSON2DSources> sources;

    JSONIndividualMatrixResponse(MatrixResult result, MatrixRequest request) {
        super(request);

        destinations = constructDestinations(result);
        sources = constructSources(result);

        for (int i = 0; i < result.getTables().length; i++) {
            if (result.getTable(i) != null) {
                switch (i) {
                    case MatrixMetricsType.DURATION -> durations = constructMetric(result.getTable(i), result);
                    case MatrixMetricsType.DISTANCE -> distances = constructMetric(result.getTable(i), result);
                    default -> {
                    }
                }
            }
        }
    }

    private Double[][] constructMetric(float[] table, MatrixResult result) {
        int sourceCount = result.getSources().length;
        int destinationCount = result.getDestinations().length;

        Double[][] constructedTable = new Double[sourceCount][destinationCount];

        for (int i = 0; i < sourceCount; i++) {
            for (int j = 0; j < destinationCount; j++) {
                double value = table[(i * destinationCount) + j];
                if (value == -1)
                    constructedTable[i][j] = null;
                else
                    constructedTable[i][j] = FormatUtility.roundToDecimals(value, 2);
            }
        }

        return constructedTable;
    }

    public Double[][] getDurations() {
        return durations;
    }

    public List<JSON2DDestinations> getDestinations() {
        return destinations;
    }

    public List<JSON2DSources> getSources() {
        return sources;
    }

    public Double[][] getDistances() {
        return distances;
    }

    public void setDistances(Double[][] distances) {
        this.distances = distances;
    }

    public void setDurations(Double[][] durations) {
        this.durations = durations;
    }

    public void setDestinations(List<JSON2DDestinations> destinations) {
        this.destinations = destinations;
    }

    public void setSources(List<JSON2DSources> sources) {
        this.sources = sources;
    }
}
