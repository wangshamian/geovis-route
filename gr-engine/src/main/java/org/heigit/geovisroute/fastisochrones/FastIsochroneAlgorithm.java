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
package org.heigit.geovisroute.fastisochrones;

import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.SPTEntry;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.fastisochrones.storage.BorderNodeDistanceStorage;
import org.heigit.geovisroute.fastisochrones.storage.EccentricityStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;

import java.util.*;

/**
 * 快速等时圈的实现
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class FastIsochroneAlgorithm extends AbstractIsochroneAlgorithm {
    private static final String NAME = "FastIsochrone";
    protected IntObjectMap<SPTEntry> startCellMap;
    protected Set<Integer> activeBorderNodes;
    protected Set<Integer> inactiveBorderNodes;
    protected Set<Integer> fullyReachableCells;
    protected Map<Integer, Map<Integer, Double>> upAndCoreGraphDistMap;
    protected Map<Integer, IntObjectMap<SPTEntry>> activeCellMaps;
    int from;
    int fromNonVirtual;

    public FastIsochroneAlgorithm(Graph graph,
                                  Weighting weighting,
                                  TraversalMode tMode,
                                  CellStorage cellStorage,
                                  IsochroneNodeStorage isochroneNodeStorage,
                                  EccentricityStorage eccentricityStorage,
                                  BorderNodeDistanceStorage borderNodeDistanceStorage,
                                  EdgeFilter additionalEdgeFilter) {
        super(graph, weighting, tMode, cellStorage, isochroneNodeStorage, eccentricityStorage, borderNodeDistanceStorage, additionalEdgeFilter);
    }

    @Override
    protected void initCollections(int size) {
        startCellMap = new GHIntObjectHashMap<>(size);
    }

    @Override
    public void init(int from, int fromNonVirtual, double isochroneLimit) {
        this.from = from;
        this.fromNonVirtual = fromNonVirtual;
        this.isochroneLimit = isochroneLimit;
        activeBorderNodes = new HashSet<>();
        inactiveBorderNodes = new HashSet<>();
        fullyReachableCells = new HashSet<>();
        upAndCoreGraphDistMap = new HashMap<>();
    }

    @Override
    void runStartCellPhase() {
        int startCell = isochroneNodeStorage.getCellId(fromNonVirtual);
        CoreRangeDijkstra coreRangeDijkstra = new CoreRangeDijkstra(graph, weighting, isochroneNodeStorage, borderNodeDistanceStorage);
        EdgeFilterSequence edgeFilterSequence = new EdgeFilterSequence();
        if (additionalEdgeFilter != null)
            edgeFilterSequence.add(additionalEdgeFilter);
        edgeFilterSequence.add(
                new CellAndBorderNodeFilter(isochroneNodeStorage,
                        startCell,
                        graph.getNodes())
        );
        coreRangeDijkstra.setEdgeFilter(edgeFilterSequence);
        coreRangeDijkstra.setIsochroneLimit(isochroneLimit);
        coreRangeDijkstra.initFrom(from);
        coreRangeDijkstra.runAlgo();
        startCellMap = coreRangeDijkstra.getFromMap();
        findFullyReachableCells(startCellMap);

        for (int inactiveBorderNode : inactiveBorderNodes) {
            startCellMap.remove(inactiveBorderNode);
            activeBorderNodes.remove(inactiveBorderNode);
        }

        for (int sweepEndNode : activeBorderNodes) {
            double dist = coreRangeDijkstra.fromMap.get(sweepEndNode).getWeightOfVisitedPath();
            int cell = isochroneNodeStorage.getCellId(sweepEndNode);
            if (cell == startCell)
                continue;
            if (!upAndCoreGraphDistMap.containsKey(cell))
                upAndCoreGraphDistMap.put(cell, new HashMap<>());
            upAndCoreGraphDistMap.get(cell).put(sweepEndNode, dist);
            startCellMap.remove(sweepEndNode);
        }
    }

    @Override
    public boolean finishedStartCellPhase() {
        return true;
    }

    /**
     * 在核心中运行 ALT 算法
     */
    @Override
    void runBorderNodePhase() {
        //此实现将起始单元内的运行和边界节点上的运行合并为一个阶段
    }

    @Override
    public boolean finishedBorderNodePhase() {
        //此实现将起始单元内的运行和边界节点上的运行合并为一个阶段
        return true;
    }

    @Override
    void runActiveCellPhase() {
        activeCellMaps = new HashMap<>(upAndCoreGraphDistMap.entrySet().size());
        activeCellMaps.put(isochroneNodeStorage.getCellId(fromNonVirtual), startCellMap);
        for (Map.Entry<Integer, Map<Integer, Double>> entry : upAndCoreGraphDistMap.entrySet()) {
            ActiveCellDijkstra activeCellDijkstra = new ActiveCellDijkstra(graph, weighting, isochroneNodeStorage, entry.getKey());
            activeCellDijkstra.setIsochroneLimit(isochroneLimit);
            //以各自已访问的权重添加所有起点
            for (int nodeId : entry.getValue().keySet()) {
                activeCellDijkstra.addInitialBordernode(nodeId, entry.getValue().get(nodeId));
            }
            activeCellDijkstra.init();
            activeCellDijkstra.runAlgo();
            activeCellMaps.put(entry.getKey(), activeCellDijkstra.getFromMap());
        }
    }

    @Override
    public boolean finishedActiveCellPhase() {
        return false;
    }

    private void findFullyReachableCells(IntObjectMap<SPTEntry> entryMap) {
        for (IntObjectCursor<SPTEntry> entry : entryMap) {
            int baseNode = entry.key;
            if (!isochroneNodeStorage.getBorderness(baseNode))
                continue;
            SPTEntry sptEntry = entry.value;
            int baseCell = isochroneNodeStorage.getCellId(baseNode);
            int eccentricity = eccentricityStorage.getEccentricity(baseNode);
            if (isWithinLimit(sptEntry, eccentricity)
                    && eccentricityStorage.getFullyReachable(baseNode)) {
                addFullyReachableCell(baseCell);
                addInactiveBorderNode(baseNode);
            } else {
                if (!getFullyReachableCells().contains(baseCell)) {
                    addActiveBorderNode(baseNode);
                }
            }
        }
    }

    /**
     * 将访问过其一定比例节点的活跃单元视为完全可达。
     *
     * @param approximation 近似因子。1 表示必须找到所有节点，0 表示无需找到任何节点。
     */
    public void approximateActiveCells(double approximation) {
        Iterator<Map.Entry<Integer, IntObjectMap<SPTEntry>>> activeCellIterator = getActiveCellMaps().entrySet().iterator();
        while (activeCellIterator.hasNext()) {
            Map.Entry<Integer, IntObjectMap<SPTEntry>> activeCell = activeCellIterator.next();
            if (activeCell.getValue().size() / (double) cellStorage.getNodesOfCell(activeCell.getKey()).size() > approximation) {
                activeCellIterator.remove();
                getFullyReachableCells().add(activeCell.getKey());
            }
        }
    }

    private boolean isWithinLimit(SPTEntry sptEntry, int eccentricity) {
        return sptEntry.getWeightOfVisitedPath() + eccentricity <= isochroneLimit;
    }

    private void addFullyReachableCell(int cellId) {
        fullyReachableCells.add(cellId);
    }

    protected void addActiveBorderNode(int nodeId) {
        activeBorderNodes.add(nodeId);
    }

    protected void addInactiveBorderNode(int nodeId) {
        inactiveBorderNodes.add(nodeId);
    }

    public Set<Integer> getFullyReachableCells() {
        return fullyReachableCells;
    }

    public IntObjectMap<SPTEntry> getStartCellMap() {
        return startCellMap;
    }

    public String getName() {
        return NAME;
    }

    public Map<Integer, IntObjectMap<SPTEntry>> getActiveCellMaps() {
        return activeCellMaps;
    }
}
