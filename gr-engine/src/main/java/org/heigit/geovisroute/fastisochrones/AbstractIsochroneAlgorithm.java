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

import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.fastisochrones.storage.BorderNodeDistanceStorage;
import org.heigit.geovisroute.fastisochrones.storage.EccentricityStorage;

/**
 * 使用分区图计算等时圈。
 * 该算法分 3 个阶段工作
 * 1. 在起始单元中向上搜索，找出到该单元内所有节点的所有距离
 * 2. 取初始单元的所有边界节点，找出所有可达的其他边界节点
 * 3. 在活动单元中向下搜索
 *
 * @author Hendrik Leuschner
 */
public abstract class AbstractIsochroneAlgorithm {
    protected final Graph graph;
    protected final Weighting weighting;
    protected final TraversalMode traversalMode;
    protected NodeAccess nodeAccess;
    protected CellStorage cellStorage;
    protected IsochroneNodeStorage isochroneNodeStorage;
    protected EccentricityStorage eccentricityStorage;
    protected BorderNodeDistanceStorage borderNodeDistanceStorage;
    protected EdgeExplorer outEdgeExplorer;
    protected EdgeFilter additionalEdgeFilter;
    int visitedCountStartCellPhase;
    int visitedCountBorderNodesPhase;
    int visitedCountActiveCellPhase;
    double isochroneLimit;
    private boolean alreadyRun;

    protected AbstractIsochroneAlgorithm(Graph graph,
                                         Weighting weighting,
                                         TraversalMode tMode,
                                         CellStorage cellStorage,
                                         IsochroneNodeStorage isochroneNodeStorage,
                                         EccentricityStorage eccentricityStorage,
                                         BorderNodeDistanceStorage borderNodeDistanceStorage,
                                         EdgeFilter additionalEdgeFilter) {
        this.weighting = weighting;
//        this.flagEncoder = weighting.getFlagEncoder();
        this.traversalMode = tMode;
        this.graph = graph;
        this.cellStorage = cellStorage;
        this.isochroneNodeStorage = isochroneNodeStorage;
        this.eccentricityStorage = eccentricityStorage;
        this.borderNodeDistanceStorage = borderNodeDistanceStorage;
        this.additionalEdgeFilter = additionalEdgeFilter;
        this.nodeAccess = graph.getNodeAccess();
//        outEdgeExplorer = graph.createEdgeExplorer(AccessFilter.outEdges(nodeAccess));

        int size = Math.min(2000, Math.max(200, graph.getNodes() / 10));
        initCollections(size);
    }

    protected abstract void initCollections(int size);

    public void init(int from, double isochroneLimit) {
        init(from, from, isochroneLimit);
    }

    public abstract void init(int from, int fromNonVirtual, double isochroneLimit);


    protected void checkAlreadyRun() {
        if (alreadyRun)
            throw new IllegalStateException("Create a new instance per call");

        alreadyRun = true;
    }


    /**
     * 运行从起点到边界节点的阶段
     */
    abstract void runStartCellPhase();

    /**
     * core 外部阶段的停止条件
     *
     * @return 是否应停止
     */
    public abstract boolean finishedStartCellPhase();

    /**
     * 在边界节点上运行算法
     */
    abstract void runBorderNodePhase();

    /**
     * core 内部阶段的停止条件
     *
     * @return 是否应停止
     */
    public abstract boolean finishedBorderNodePhase();

    /**
     * 开始运行为活动单元中的算法阶段
     */
    abstract void runActiveCellPhase();

    /**
     * 活动单元阶段的停止条件
     *
     * @return 是否应停止
     */
    public abstract boolean finishedActiveCellPhase();

    protected boolean finished() {
        return finishedActiveCellPhase();
    }

    protected void runAlgo() {
        runStartCellPhase();

        runBorderNodePhase();

        runActiveCellPhase();
    }

    public void calcIsochroneNodes(int from, double isochroneLimit) {
        checkAlreadyRun();
        init(from, isochroneLimit);
        runAlgo();
    }

    /**
     * 为一组给定的（虚拟）起点节点和非虚拟基础节点计算节点。对于无法处理虚拟节点的预计算信息来说是必要的。
     *
     * @param from           虚拟起点节点
     * @param fromNonVirtual 最接近虚拟起点节点的真实节点
     * @param isochroneLimit 限制
     */
    public void calcIsochroneNodes(int from, int fromNonVirtual, double isochroneLimit) {
        checkAlreadyRun();
        init(from, fromNonVirtual, isochroneLimit);
        runAlgo();
    }

    public int getVisitedNodes() {
        return getVisitedNodesPhase1() + getVisitedNodesPhase2() + getVisitedNodesPhase3();
    }

    public int getVisitedNodesPhase1() {
        return visitedCountStartCellPhase;
    }

    public int getVisitedNodesPhase2() {
        return visitedCountBorderNodesPhase;
    }

    public int getVisitedNodesPhase3() {
        return visitedCountActiveCellPhase;
    }
}
