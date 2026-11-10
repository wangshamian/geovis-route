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
package org.heigit.geovisroute.matrix.algorithms.rphast;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.RoutingCHGraph;
import org.heigit.geovisroute.matrix.*;
import org.heigit.geovisroute.matrix.algorithms.AbstractMatrixAlgorithm;
import org.heigit.geovisroute.routing.algorithms.RPHASTAlgorithm;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.MultiTreeSPEntry;

import java.util.ArrayList;
import java.util.List;

public class RPHASTMatrixAlgorithm extends AbstractMatrixAlgorithm {
    private MultiTreeMetricsExtractor pathMetricsExtractor;
    private RoutingCHGraph chGraph;

    //        @Override
    public void init(MatrixRequest req, GraphHopper gh, RoutingCHGraph chGraph, String vehicleName, Weighting weighting) {
        //TODO 重构：检查是否需要基础图。可能不需要。
        super.init(req, gh, chGraph.getBaseGraph(), vehicleName, weighting);
        this.chGraph = chGraph;

        pathMetricsExtractor = new MultiTreeMetricsExtractor(req.getMetrics(), chGraph, vehicleName, weighting,
                req.getUnits());
    }

    @Override
    public MatrixResult compute(MatrixLocations srcData, MatrixLocations dstData, int metrics) throws Exception {
        // 当 dstData.size > srcData.size 时搜索效率更高，因此检查是否需要交换二者
        boolean swap = checkSwapSrcDst(srcData, dstData);
        if (swap) {
            MatrixLocations tmp = srcData;
            srcData = dstData;
            dstData = tmp;
        }

        float[] times = null;
        float[] distances = null;
        float[] weights = null;

        int tableSize = srcData.size() * dstData.size();
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DURATION))
            times = new float[tableSize];
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DISTANCE))
            distances = new float[tableSize];
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.WEIGHT))
            weights = new float[tableSize];

        if (!srcData.hasValidNodes() || !dstData.hasValidNodes()) {
            for (int srcIndex = 0; srcIndex < srcData.size(); srcIndex++)
                pathMetricsExtractor.setEmptyValues(srcIndex, dstData, times, distances, weights);
        } else {
            RPHASTAlgorithm algorithm = new RPHASTAlgorithm(chGraph, chGraph.getWeighting(), TraversalMode.NODE_BASED, swap);
            algorithm.setMaxVisitedNodes(this.maxVisitedNodes);

            int[] srcIds = getValidNodeIds(srcData.getNodeIds());
            int[] destIds = getValidNodeIds(dstData.getNodeIds());

            algorithm.prepare(srcIds, destIds);

            MultiTreeSPEntry[] destTrees = algorithm.calcPaths(srcIds, destIds);

            MultiTreeSPEntry[] originalDestTrees = new MultiTreeSPEntry[dstData.size()];

            int j = 0;
            for (int i = 0; i < dstData.size(); i++) {
                if (dstData.getNodeIds()[i] != -1) {
                    originalDestTrees[i] = destTrees[j];
                    ++j;
                } else {
                    originalDestTrees[i] = null;
                }
            }
            pathMetricsExtractor.setSwap(swap);
            pathMetricsExtractor.calcValues(originalDestTrees, srcData, dstData, times, distances, weights);
        }

        // 如果之前进行过交换，需要重新映射源和目标
        if (swap) {
            MatrixLocations tmp = srcData;
            srcData = dstData;
            dstData = tmp;
            float[][] results = swapResults(srcData, dstData, times, distances, weights);
            times = results[0];
            distances = results[1];
            weights = results[2];
        }

        MatrixResult mtxResult = new MatrixResult(srcData.getLocations(), dstData.getLocations());

        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DURATION))
            mtxResult.setTable(MatrixMetricsType.DURATION, times);
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.DISTANCE))
            mtxResult.setTable(MatrixMetricsType.DISTANCE, distances);
        if (MatrixMetricsType.isSet(metrics, MatrixMetricsType.WEIGHT))
            mtxResult.setTable(MatrixMetricsType.WEIGHT, weights);

        return mtxResult;
    }

    private int[] getValidNodeIds(int[] nodeIds) {
        List<Integer> nodeList = new ArrayList<>();
        for (int dst : nodeIds) {
            if (dst != -1)
                nodeList.add(dst);

        }

        int[] res = new int[nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++)
            res[i] = nodeList.get(i);

        return res;
    }

    /**
     * 当源数量少、目标数量多时，搜索比反过来更高效。
     * 如果源多于目标，则将其交换，并且所有计算都以反向方式进行。
     * 最终结果会被还原交换以返回正确结果。
     *
     * @param srcData 原始源数据
     * @param dstData 原始目标数据
     * @return 源是否多于目标
     */
    private boolean checkSwapSrcDst(MatrixLocations srcData, MatrixLocations dstData) {
        return (srcData.size() > dstData.size());
    }

    /**
     * 在 src 与 dst 被交换的情况下，反转结果矩阵（以扁平数组表示）
     *
     * @param srcData   原始未交换的源数据
     * @param dstData   原始未交换的目标数据
     * @param times     已交换的结果数组
     * @param distances 已交换的结果数组
     * @param weights   已交换的结果数组
     * @return 未交换的结果数组 [times, distances, weights]
     */
    private float[][] swapResults(MatrixLocations srcData, MatrixLocations dstData, float[] times, float[] distances, float[] weights) {
        boolean hasTimes = times != null;
        boolean hasDistances = distances != null;
        boolean hasWeights = weights != null;
        float[] newTimes = new float[hasTimes ? times.length : 0];
        float[] newDistances = new float[hasDistances ? distances.length : 0];
        float[] newWeights = new float[hasWeights ? weights.length : 0];

        int i = 0;
        int srcSize = srcData.size();
        int dstSize = dstData.size();
        for (int dst = 0; dst < dstSize; dst++) {
            for (int src = 0; src < srcSize; src++) {
                int index = dst + src * dstSize;
                if (hasTimes)
                    newTimes[index] = times[i];
                if (hasDistances)
                    newDistances[index] = distances[i];
                if (hasWeights)
                    newWeights[index] = weights[i];
                i++;
            }
        }
        return new float[][]{newTimes, newDistances, newWeights};
    }
}
