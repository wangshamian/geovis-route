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
package org.heigit.geovisroute.matrix;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.common.DistanceUnit;
import org.heigit.geovisroute.common.ServiceRequest;
import org.heigit.geovisroute.exceptions.InternalServerException;
import org.heigit.geovisroute.exceptions.MaxVisitedNodesExceededException;
import org.heigit.geovisroute.exceptions.PointNotFoundException;
import org.heigit.geovisroute.matrix.algorithms.core.CoreMatrixAlgorithm;
import org.heigit.geovisroute.matrix.algorithms.dijkstra.DijkstraMatrixAlgorithm;
import org.heigit.geovisroute.matrix.algorithms.rphast.RPHASTMatrixAlgorithm;
import org.heigit.geovisroute.routing.RouteSearchContext;
import org.heigit.geovisroute.routing.RoutingProfile;
import org.heigit.geovisroute.routing.RoutingProfileType;
import org.heigit.geovisroute.routing.WeightingMethod;
import org.heigit.geovisroute.routing.graphhopper.extensions.GREdgeFilterFactory;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRWeightingFactory;
import org.heigit.geovisroute.util.ProfileTools;
import org.heigit.geovisroute.util.TemporaryUtilShelter;
import org.locationtech.jts.geom.Coordinate;

public class MatrixRequest extends ServiceRequest {
    private String profileName;
    private int profileType = -1;
    private Coordinate[] sources;
    private Coordinate[] destinations;
    private int metrics = MatrixMetricsType.DURATION;
    private int weightingMethod;
    private DistanceUnit units = DistanceUnit.METERS;
    private boolean resolveLocations = false;
    private boolean flexibleMode = false;
    private String algorithm;
    private MatrixSearchParameters searchParameters;
    private double maximumSearchRadius;
    private int maximumVisitedNodes;
    private boolean hasInfiniteUTurnCosts;

    public MatrixRequest(double maximumSearchRadius, int maximumVisitedNodes, double uTurnCosts) {
        this.maximumSearchRadius = maximumSearchRadius;
        this.maximumVisitedNodes = maximumVisitedNodes;
        this.hasInfiniteUTurnCosts = uTurnCosts == Weighting.INFINITE_U_TURN_COSTS;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public Coordinate[] getSources() {
        return sources;
    }

    public void setSources(Coordinate[] sources) {
        this.sources = sources;
    }

    public Coordinate[] getDestinations() {
        return destinations;
    }

    public void setDestinations(Coordinate[] destinations) {
        this.destinations = destinations;
    }

    public int getMetrics() {
        return metrics;
    }

    public void setMetrics(int metrics) {
        this.metrics = metrics;
    }

    public boolean getResolveLocations() {
        return resolveLocations;
    }

    public void setResolveLocations(boolean resolveLocations) {
        this.resolveLocations = resolveLocations;
    }

    public int getProfileType() {
        return profileType;
    }

    public void setProfileType(int profile) {
        profileType = profile;
    }

    public DistanceUnit getUnits() {
        return units;
    }

    public void setUnits(DistanceUnit units) {
        this.units = units;
    }

    public int getTotalNumberOfLocations() {
        return destinations.length * sources.length;
    }

    public int getWeightingMethod() {
        return weightingMethod;
    }

    /**
     * 返回 weightingMethod，若方法为 UNKNOWN 则返回合理的默认值。
     */
    public int getWeightingMethodOrDefault() {
        return weightingMethod == WeightingMethod.UNKNOWN ? WeightingMethod.RECOMMENDED : weightingMethod;
    }

    public void setWeightingMethod(int weightingMethod) {
        this.weightingMethod = weightingMethod;
    }

    public boolean getFlexibleMode() {
        return flexibleMode;
    }

    public void setFlexibleMode(boolean flexibleMode) {
        this.flexibleMode = flexibleMode;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public double getMaximumSearchRadius() {
        return maximumSearchRadius;
    }

    public void setMaximumSearchRadius(double radius) {
        this.maximumSearchRadius = radius;
    }

    public int getMaximumVisitedNodes() {
        return maximumVisitedNodes;
    }

    public void setMaximumVisitedNodes(int maximumVisitedNodes) {
        this.maximumVisitedNodes = maximumVisitedNodes;
    }

    public void setSearchParameters(MatrixSearchParameters searchParameters) {
        this.searchParameters = searchParameters;
    }

    public MatrixSearchParameters getSearchParameters() {
        return searchParameters;
    }

    public boolean isValid() {
        return !(sources == null && destinations == null);
    }

    public boolean hasInfiniteUTurnCosts() {
        return hasInfiniteUTurnCosts;
    }

    public void setInfiniteUTurnCosts(boolean hasInfiniteUTurnCosts) {
        this.hasInfiniteUTurnCosts = hasInfiniteUTurnCosts;
    }

    /**
     * 使用三种可用方法之一，根据请求计算 NxM 矩阵。
     * 出于性能考虑，视请求条件优先选择 RPHAST，其次是 CoreMatrix，最后是 DijkstraMatrix。
     *
     * @param routingProfile@return 一个 MatrixResult 对象，可能包含 N 个和 M 个输入位置所有组合的时间与距离值
     * @throws Exception
     */
    public MatrixResult computeMatrix(RoutingProfile routingProfile) throws Exception {
        GraphHopper gh = routingProfile.getGraphhopper();
        String encoderName = RoutingProfileType.getEncoderName(getProfileType());
        PMap hintsMap = new PMap();
        ProfileTools.setWeightingMethod(hintsMap, getWeightingMethodOrDefault(), getProfileType(), false);
        ProfileTools.setWeighting(hintsMap, getWeightingMethodOrDefault(), getProfileType(), false);
        String chProfileName = ProfileTools.makeProfileName(encoderName, hintsMap.getString("weighting", ""), false);
        String coreProfileName = ProfileTools.makeProfileName(encoderName, hintsMap.getString("weighting", ""), true);

        //TODO 重构：由于算法选择的检查必须在此处再次执行，可能应完全移除 MatrixAlgorithmFactory。或者优雅地合并为单个检查
        try {
            // RPHAST
            if (!getFlexibleMode() && gh.getCHPreparationHandler().isEnabled() && routingProfile.hasCHProfile(chProfileName)) {
                return computeRPHASTMatrix(gh, encoderName, chProfileName);
            }
            // Core
            else if (getSearchParameters().getDynamicSpeeds() && routingProfile.getGraphhopper().isCoreAvailable(coreProfileName)) {
                return computeCoreMatrix(gh, encoderName, hintsMap, coreProfileName, routingProfile);
            }
            // Dijkstra
            else {
                // 使用 chProfileName（不含转向代价），因为 Dijkstra 基于节点，不使用转向限制。
                return computeDijkstraMatrix(gh, encoderName, hintsMap, chProfileName);
            }
        } catch (PointNotFoundException e) {
            throw e;
        } catch (MaxVisitedNodesExceededException e) {
            throw new InternalServerException(MatrixErrorCodes.MAX_VISITED_NODES_EXCEEDED, "Unable to compute a distance/duration matrix: " + e.getMessage());
        } catch (Exception ex) {
            throw new InternalServerException(MatrixErrorCodes.UNKNOWN, "Unable to compute a distance/duration matrix: " + ex.getMessage());
        }
    }

    /**
     * 基于收缩层次图，使用 RPHAST 算法计算矩阵。速度很快，但不够灵活。
     *
     * @param gh
     * @param vehicle
     * @param profileName
     * @return
     * @throws Exception
     */
    private MatrixResult computeRPHASTMatrix(GraphHopper gh, String vehicle, String profileName) throws Exception {
        RoutingCHGraph routingCHGraph = gh.getBaseGraph().getRoutingCHGraph(profileName);
        MatrixSearchContextBuilder builder = new MatrixSearchContextBuilder(gh.getBaseGraph(), gh.getEncodingManager(), gh.getLocationIndex(), AccessFilter.allEdges(new SimpleBooleanEncodedValue(vehicle)), getResolveLocations());
        MatrixSearchContext mtxSearchCntx = builder.create((BaseGraph) routingCHGraph.getBaseGraph(), routingCHGraph, routingCHGraph.getWeighting(), profileName, getSources(), getDestinations(), getMaximumSearchRadius());

        RPHASTMatrixAlgorithm algo = new RPHASTMatrixAlgorithm();
        algo.init(this, gh, mtxSearchCntx.getRoutingCHGraph(), vehicle, routingCHGraph.getWeighting());
        return algo.compute(mtxSearchCntx.getSources(), mtxSearchCntx.getDestinations(), getMetrics());
    }

    /**
     * 基于普通图计算矩阵。速度较慢，但在请求参数方面高度灵活。
     *
     * @param gh
     * @param encodeName
     * @param hintsMap
     * @param profileName
     * @return
     * @throws Exception
     */
    private MatrixResult computeDijkstraMatrix(GraphHopper gh, String encodeName,PMap hintsMap, String profileName) throws Exception {
        Graph graph = gh.getBaseGraph();
        EncodingManager encodingManager = gh.getEncodingManager();
        Weighting weighting = new GRWeightingFactory(gh.getBaseGraph(), gh.getEncodingManager()).createWeighting(gh.getProfile(profileName), hintsMap, false);
        BooleanEncodedValue accessEnc = encodingManager.getBooleanEncodedValue(VehicleAccess.key(encodeName));
        MatrixSearchContextBuilder builder = new MatrixSearchContextBuilder(gh.getBaseGraph(), gh.getEncodingManager(), gh.getLocationIndex(), AccessFilter.allEdges(accessEnc), getResolveLocations());
        MatrixSearchContext mtxSearchCntx = builder.create((BaseGraph) graph, null, weighting, profileName, getSources(), getDestinations(), getMaximumSearchRadius());

        DijkstraMatrixAlgorithm algo = new DijkstraMatrixAlgorithm();
        algo.init(this, gh, mtxSearchCntx.getGraph(), encodeName, weighting);
        return algo.compute(mtxSearchCntx.getSources(), mtxSearchCntx.getDestinations(), getMetrics());
    }

    /**
     * 基于核心收缩图计算矩阵，速度比 RPHAST 慢，但具备核心的全部灵活性
     *
     * @param gh
     * @param encodeName
     * @param hintsMap
     * @param profileName
     * @param routingProfile
     * @return
     */
    private MatrixResult computeCoreMatrix(GraphHopper gh, String encodeName,  PMap hintsMap, String profileName, RoutingProfile routingProfile) throws Exception {
        Weighting weighting = new GRWeightingFactory(gh.getBaseGraph(), gh.getEncodingManager()).createWeighting(gh.getProfile(profileName), hintsMap, false);
        GRGraph baseGraph = (GRGraph) gh.getBaseGraph();
        RoutingCHGraph graph = baseGraph.getCoreGraph(profileName);
        RouteSearchContext searchCntx = TemporaryUtilShelter.createSearchContext(getSearchParameters(), routingProfile);
        PMap additionalHints = searchCntx.getProperties();
        EdgeFilter edgeFilter = new GREdgeFilterFactory().createEdgeFilter(additionalHints, encodeName, gh.getBaseGraph(), gh.getEncodingManager());

        MatrixSearchContextBuilder builder = new MatrixSearchContextBuilder(gh.getBaseGraph(), gh.getEncodingManager(), gh.getLocationIndex(), edgeFilter, getResolveLocations());
        MatrixSearchContext mtxSearchCntx = builder.create((BaseGraph) graph.getBaseGraph(), graph, weighting, profileName, getSources(), getDestinations(), getMaximumSearchRadius());

        CoreMatrixAlgorithm algo = new CoreMatrixAlgorithm();
        algo.init(this, gh, mtxSearchCntx.getRoutingCHGraph(), encodeName, weighting, edgeFilter);
        return algo.compute(mtxSearchCntx.getSources(), mtxSearchCntx.getDestinations(), getMetrics());
    }
}
