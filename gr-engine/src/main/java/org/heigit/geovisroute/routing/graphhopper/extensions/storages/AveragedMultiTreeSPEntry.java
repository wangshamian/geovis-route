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
package org.heigit.geovisroute.routing.graphhopper.extensions.storages;

/**
 * 具有多个字段、用于包含多个来源信息的最短路径树。
 * 权重为所有非无限条目的平均值，用于在优先队列中排序。
 * 相比最小权重，平均权重可带来更好的性能。
 *
 * @author Hendrik Leuschner
 */
public class AveragedMultiTreeSPEntry extends MultiTreeSPEntry {
    private double weight = Double.POSITIVE_INFINITY;

    public AveragedMultiTreeSPEntry(int adjNode, int edgeId, double edgeWeight, boolean updated, AveragedMultiTreeSPEntry parent, int numTrees) {
        super(adjNode, edgeId, edgeWeight, updated, parent, numTrees);
        updateWeights();
    }

    public void setSubItemOriginalEdgeIds(int newOriginalEdgeId) {
        for (int i = 0; i < getSize(); i++) {
            getItem(i).setOriginalEdge(newOriginalEdgeId);
        }
    }


    @Override
    public void updateWeights() {
        double averageWeight = 0;
        int numNonInfiniteWeights = 0;
        for (int i = 0; i < getSize(); i++) {
            MultiTreeSPEntryItem item = getItem(i);
            double itemWeight = item.getWeight();
            if (itemWeight != Double.POSITIVE_INFINITY) {
                averageWeight += itemWeight;
                numNonInfiniteWeights++;
            }

        }
        if (numNonInfiniteWeights == 0)
            weight = Double.POSITIVE_INFINITY;
        else
            weight = averageWeight / numNonInfiniteWeights;
    }

    @Override
    public int compareTo(MultiTreeSPEntry other) {
        AveragedMultiTreeSPEntry o = (AveragedMultiTreeSPEntry) other;
        if (weight < o.weight)
            return -1;

        // 假设不存在 NaN 和 -0
        return weight > o.weight ? 1 : 0;
    }

    @Override
    public String toString() {
        return "adjNode: " + getAdjNode() + ",  average weight: " + weight;
    }
}
