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

import com.graphhopper.*;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.LMProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.gtfs.GraphHopperGtfs;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.routing.Router;
import com.graphhopper.routing.RouterConfig;
import com.graphhopper.routing.WeightingFactory;
import com.graphhopper.routing.ch.CHPreparationHandler;
import com.graphhopper.routing.lm.LMConfig;
import com.graphhopper.routing.lm.LMPreparationHandler;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.lm.PrepareLandmarks;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EdgeFilterFactory;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.CHConfig;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.*;
import com.graphhopper.util.details.PathDetailsBuilderFactory;
import org.geotools.feature.SchemaException;
import org.heigit.geovisroute.common.TravelRangeType;
import org.heigit.geovisroute.config.EngineProperties;
import org.heigit.geovisroute.config.profile.ProfileProperties;
import org.heigit.geovisroute.fastisochrones.Contour;
import org.heigit.geovisroute.fastisochrones.Eccentricity;
import org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneFactory;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.routing.AvoidFeatureFlags;
import org.heigit.geovisroute.routing.RouteSearchContext;
import org.heigit.geovisroute.routing.graphhopper.extensions.core.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.AvoidFeaturesEdgeFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.EdgeFilterSequence;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.HeavyVehicleEdgeFilter;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.LMEdgeFilterSequence;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.GRGraphManager;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.GraphStorageUtils;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.HeavyVehicleAttributesGraphStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.TrafficGraphStorage;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.builders.GraphStorageBuilder;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.builders.HereTrafficGraphStorageBuilder;
import org.heigit.geovisroute.routing.graphhopper.extensions.util.GRParameters;
import org.heigit.geovisroute.routing.graphhopper.extensions.weighting.HgvAccessWeighting;
import org.heigit.geovisroute.util.AppInfo;
import org.heigit.geovisroute.util.CoordTools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;


public class GRGraphHopper extends GraphHopperGtfs {
    private static final Logger LOGGER = LoggerFactory.getLogger(GRGraphHopper.class);

    private GraphProcessContext processContext;
    private EngineProperties engineProperties;
    private ProfileProperties profileProperties;
    private HashMap<Long, ArrayList<Integer>> osmId2EdgeIds; // 一个 osm id 可能对应多条边
    private HashMap<Integer, Long> tmcEdges;
    private Eccentricity eccentricity;

    private int minNetworkSize = 200;

    private final CorePreparationHandler corePreparationHandler = new CorePreparationHandler();
    private final CoreLMPreparationHandler coreLMPreparationHandler = new CoreLMPreparationHandler();
    private final FastIsochroneFactory fastIsochroneFactory = new FastIsochroneFactory();

    private GRGraphManager orsGraphManager;

    // ORS-GH MOD START
    protected WeightingFactory weightingFactory;
    protected GRGraphFactory graphFactory;

    public void setWeightingFactory(WeightingFactory weightingFactory) {
        this.weightingFactory = weightingFactory;
    }

    public void setORSGraphFactory(GRGraphFactory graphFactory) {
        this.graphFactory = graphFactory;
    }
    // ORS-GH MOD END

    public GRGraphManager getOrsGraphManager() {
        return this.orsGraphManager;
    }

    public void setOrsGraphManager(GRGraphManager orsGraphManager) {
        this.orsGraphManager = orsGraphManager;
    }

    public GraphHopperConfig getConfig() {
        return config;
    }

    public ProfileProperties getProfileProperties() {
        return profileProperties;
    }

    private GraphHopperConfig config;

    public GRGraphHopper(GraphProcessContext processContext, EngineProperties engineProperties, ProfileProperties profileProperties,GraphHopperConfig ghConfig) {
        super(ghConfig);
        this.processContext = processContext;
        this.engineProperties = engineProperties;
        this.profileProperties = profileProperties;
    }

//    public GRGraphHopper() {
//        // used to initialize tests more easily without the need to create GraphProcessContext etc. when they're anyway not used in the tested functions.
//    }

    @Override
    public GraphHopper init(GraphHopperConfig ghConfig) {
        GraphHopper ret = super.init(ghConfig);

        if (ghConfig instanceof GRGraphHopperConfig orsConfig) {
            corePreparationHandler.init(orsConfig);
            coreLMPreparationHandler.init(orsConfig);
        }

        fastIsochroneFactory.init(ghConfig);

        minNetworkSize = ghConfig.getInt("prepare.min_network_size", minNetworkSize);
        config = ghConfig;

        return ret;
    }

    @Override
    protected void cleanUp() {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("call cleanUp for '%s' ".formatted(getGraphHopperLocation()));
        BaseGraph baseGraph = getBaseGraph();
        if (baseGraph != null) {
            if (LOGGER.isInfoEnabled())
                LOGGER.info("graph %s, details:%s".formatted(baseGraph, baseGraph.toDetailsString()));
            int prevNodeCount = baseGraph.getNodes();
            int ex = baseGraph.getAllEdges().length();
//            List<FlagEncoder> list = getEncodingManager().fetchEdgeEncoders();
//            if (LOGGER.isDebugEnabled())
//                LOGGER.debug("will create PrepareRoutingSubnetworks with: NodeCountBefore: '%d' getAllEdges().getMaxId(): '%d' List<FlagEncoder>: '%s' minNetworkSize: '%d'".formatted(prevNodeCount, ex, list, minNetworkSize)
//                );
            getProperties().put("elevation", hasElevation());
        } else {
            LOGGER.info("graph GraphHopperStorage is null?!");
        }
        super.cleanUp();
    }

//    protected OSMReader createOSMReader() {
//        return new GROSMReader(getBaseGraph(),getOSMParsers(), config, processContext);
//    }

    @Override
    public GraphHopper importOrLoad() {
//        if (isFullyLoaded()) {
//            throw new IllegalStateException("graph is already successfully loaded");
//        }

        GRGraphHopper gh = (GRGraphHopper) super.importOrLoad();
        AppInfo.setGraphDate(getProperties().get("datareader.import.date"));
        AppInfo.setOsmDate(getProperties().get("datareader.data.date"));

        writeOrsGraphBuildInfoFileIfNotExists(gh);

        if ((tmcEdges != null) && (osmId2EdgeIds != null)) {
            java.nio.file.Path path = Paths.get(gh.getGraphHopperLocation(), "edges_ors_traffic");

            if ((tmcEdges.size() == 0) || (osmId2EdgeIds.size() == 0)) {
                // 尝试从文件加载 TMC 边。

                File file = path.toFile();
                if (file.exists()) {
                    try (FileInputStream fis = new FileInputStream(path.toString());
                         ObjectInputStream ois = new ObjectInputStream(fis)) {
                        tmcEdges = (HashMap<Integer, Long>) ois.readObject();
                        osmId2EdgeIds = (HashMap<Long, ArrayList<Integer>>) ois.readObject();
                        LOGGER.info("Serialized HashMap data is saved in trafficEdges");
                    } catch (IOException ioe) {
                        LOGGER.error(Arrays.toString(ioe.getStackTrace()));
                    } catch (ClassNotFoundException c) {
                        LOGGER.error("Class not found");
                        LOGGER.error(Arrays.toString(c.getStackTrace()));
                    }
                }
            } else {
                // 如有需要则保存 TMC 边。
                try (FileOutputStream fos = new FileOutputStream(path.toString());
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(tmcEdges);
                    oos.writeObject(osmId2EdgeIds);
                    LOGGER.info("Serialized HashMap data is saved in trafficEdges");
                } catch (IOException ioe) {
                    LOGGER.error(Arrays.toString(ioe.getStackTrace()));
                }
            }
        }

        return gh;
    }

    private void writeOrsGraphBuildInfoFileIfNotExists(GRGraphHopper gh) {
        orsGraphManager.writeOrsGraphBuildInfoFileIfNotExists(gh);
    }

    protected Router doCreateRouter(GRGraph orsGraph, LocationIndex locationIndex, Map<String, Profile> profilesByName,
                                    PathDetailsBuilderFactory pathBuilderFactory, TranslationMap trMap, RouterConfig routerConfig,
                                    WeightingFactory weightingFactory, Map<String, RoutingCHGraph> chGraphs, Map<String, LandmarkStorage> landmarks) {
        GRRouter r = new GRRouter(orsGraph, encodingManager,locationIndex, profilesByName, pathBuilderFactory, trMap, routerConfig, weightingFactory, chGraphs, landmarks);
        r.setEdgeFilterFactory((EdgeFilterFactory) new GREdgeFilterFactory());
//        r.setPathProcessorFactory(pathProcessorFactory);

        if (!(orsGraph instanceof GRGraph))
            throw new IllegalStateException("Expected an instance of GRGraph");

        Map<String, RoutingCHGraph> coreGraphs = new LinkedHashMap<>();
        for (com.graphhopper.config.CHProfile chProfile : corePreparationHandler.getCHProfiles()) {
            String chGraphName = corePreparationHandler.getPreparation(chProfile.getProfile()).getCHConfig().getName();
            coreGraphs.put(chProfile.getProfile(), ((GRGraph) orsGraph).getCoreGraph(chGraphName));
        }
        r.setCoreGraphs(coreGraphs);

        Map<String, PrepareCoreLandmarks> coreLandmarks = new LinkedHashMap<>();
        for (PrepareLandmarks preparation : coreLMPreparationHandler.getPreparations()) {
            coreLandmarks.put(preparation.getLMConfig().getName(), (PrepareCoreLandmarks) preparation);
        }
        r.setCoreLandmarks(coreLandmarks);

        return r;
    }

    @Override
    protected WeightingFactory createWeightingFactory() {
        return new GRWeightingFactory(getBaseGraph(), getEncodingManager());
    }

    public GHResponse constructFreeHandRoute(GHRequest request) {
        LineString directRouteGeometry = constructFreeHandRouteGeometry(request);
        ResponsePath directRoutePathWrapper = constructFreeHandRoutePathWrapper(directRouteGeometry);
        GHResponse directRouteResponse = new GHResponse();
        directRouteResponse.add(directRoutePathWrapper);
        directRouteResponse.getHints().putObject("skipped_segment", true);
        return directRouteResponse;
    }

    private ResponsePath constructFreeHandRoutePathWrapper(LineString lineString) {
        ResponsePath responsePath = new ResponsePath();
        PointList pointList = new PointList();
        PointList startPointList = new PointList();
        PointList endPointList = new PointList();
        PointList wayPointList = new PointList();
        Coordinate startCoordinate = lineString.getCoordinateN(0);
        Coordinate endCoordinate = lineString.getCoordinateN(1);
        double distance = CoordTools.calcDistHaversine(startCoordinate.x, startCoordinate.y, endCoordinate.x, endCoordinate.y);
        pointList.add(lineString.getCoordinateN(0).x, lineString.getCoordinateN(0).y);
        pointList.add(lineString.getCoordinateN(1).x, lineString.getCoordinateN(1).y);
        wayPointList.add(lineString.getCoordinateN(0).x, lineString.getCoordinateN(0).y);
        wayPointList.add(lineString.getCoordinateN(1).x, lineString.getCoordinateN(1).y);
        startPointList.add(lineString.getCoordinateN(0).x, lineString.getCoordinateN(0).y);
        endPointList.add(lineString.getCoordinateN(1).x, lineString.getCoordinateN(1).y);
        Translation translation = new TranslationMap.TranslationHashMap(new Locale(""));
        InstructionList instructions = new InstructionList(translation);
        Instruction startInstruction = new Instruction(Instruction.REACHED_VIA, "free hand route", startPointList);
        Instruction endInstruction = new Instruction(Instruction.FINISH, "end of free hand route", endPointList);
        instructions.add(0, startInstruction);
        instructions.add(1, endInstruction);
        responsePath.setDistance(distance);
        responsePath.setAscend(0.0);
        responsePath.setDescend(0.0);
        responsePath.setTime(0);
        responsePath.setInstructions(instructions);
        responsePath.setWaypoints(wayPointList);
        responsePath.setPoints(pointList);
        responsePath.setRouteWeight(0.0);
        responsePath.setDescription(new ArrayList<>());
        responsePath.setImpossible(false);
        startInstruction.setDistance(distance);
        startInstruction.setTime(0);
        return responsePath;
    }

    private LineString constructFreeHandRouteGeometry(GHRequest request) {
        Coordinate start = new Coordinate();
        Coordinate end = new Coordinate();
        start.x = request.getPoints().get(0).getLat();
        start.y = request.getPoints().get(0).getLon();
        end.x = request.getPoints().get(1).getLat();
        end.y = request.getPoints().get(1).getLon();
        Coordinate[] coords = new Coordinate[]{start, end};
        return new GeometryFactory().createLineString(coords);
    }

    private void matchTraffic() {
        // 执行图扩展的后处理
        // 预留给需要已完成初始化的图（例如用于匹配）的处理流程
        if (getBaseGraph() != null && processContext != null && processContext.getStorageBuilders() != null) {
            for (GraphStorageBuilder graphStorageBuilder : processContext.getStorageBuilders()) {
                if (graphStorageBuilder instanceof HereTrafficGraphStorageBuilder storageBuilder) {
                    try {
                        storageBuilder.postProcess(this);
                    } catch (SchemaException e) {
                        LOGGER.error("Error building the here traffic storage.");
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void addTrafficSpeedCalculator(LMPreparationHandler lmPreparationHandler) {
        if (isTrafficEnabled())
            GRWeightingFactory.addTrafficSpeedCalculator(lmPreparationHandler.getWeightings(), getBaseGraph());
    }

    /**
     * 执行预处理并创建位置索引
     *
     * @param closeEarly 尽可能早地释放资源
     */
    @Override
    protected void postProcessing(boolean closeEarly) {
        super.postProcessing(closeEarly);

        //创建 core
        BaseGraph baseGraph = getBaseGraph();
        if (corePreparationHandler.isEnabled())
            corePreparationHandler.setProcessContext(processContext).createPreparations(baseGraph);
        if (isCorePrepared()) {
            // 检查已加载的 profile
            for (CHProfile profile : corePreparationHandler.getCHProfiles()) {
                if (!getProfileVersion(profile.getProfile()).isEmpty() && !getProfileVersion(profile.getProfile()).equals("" + profilesByName.get(profile.getProfile()).getVersion()))
                    throw new IllegalArgumentException("Core preparation of " + profile.getProfile() + " already exists in storage and doesn't match configuration");
            }
        } else {
            prepareCore(closeEarly);
        }

        //在 core 中创建 landmark
//        if (coreLMPreparationHandler.isEnabled()) {
//            loadOrPrepareCoreLM();
//        }

        if (fastIsochroneFactory.isEnabled()) {
            EdgeFilterSequence partitioningEdgeFilter = new EdgeFilterSequence();
            try {
                partitioningEdgeFilter.add(new AvoidFeaturesEdgeFilter(AvoidFeatureFlags.FERRIES, getBaseGraph()));
            } catch (Exception e) {
                LOGGER.debug(e.getLocalizedMessage());
            }
            fastIsochroneFactory.createPreparation(baseGraph, encodingManager, partitioningEdgeFilter);

            if (!isPartitionPrepared())
                preparePartition();
            else {
                fastIsochroneFactory.setExistingStorages();
                fastIsochroneFactory.getCellStorage().loadExisting();
                fastIsochroneFactory.getIsochroneNodeStorage().loadExisting();
            }
//            //No fast isochrones without partition
//            if (isPartitionPrepared()) {
//                // Initialize edge filter sequence for fast isochrones
//                calculateContours();
//                List<Profile> profiles = fastIsochroneFactory.getFastIsochroneProfiles();
//                for (Profile profile : profiles) {
//                    Weighting weighting = ((GRWeightingFactory) createWeightingFactory()).createIsochroneWeighting(profile);
//                    for (FlagEncoder encoder : super.getEncodingManager().fetchEdgeEncoders()) {
//                        calculateCellProperties(weighting, partitioningEdgeFilter, encoder, fastIsochroneFactory.getIsochroneNodeStorage(), fastIsochroneFactory.getCellStorage());
//                    }
//                }
//            }
        }
    }

//    @Override
//    protected void postProcessingHook() {
//        matchTraffic();
//
//        if (getLMPreparationHandler().isEnabled())
//            addTrafficSpeedCalculator(getLMPreparationHandler());
//    }

    //TODO 重构：这与 RoutingProfile 中的代码重复，可能应移到某个状态维护类中。
    private boolean hasCHProfile(String profileName) {
        return contains(getBaseGraph().getCHGraphNames(), profileName);
    }

    private boolean hasCoreProfile(String profileName) {
        if (getBaseGraph() instanceof BaseGraph) {
            List<String> profiles = ((GRGraph) getBaseGraph()).getCoreGraphNames();
            return contains(profiles, profileName);
        }
        return false;
    }

    private boolean hasLMProfile(String profileName) {
        List<String> profiles = getLMPreparationHandler().getLMConfigs().stream().map(LMConfig::getName).collect(Collectors.toList());
        return contains(profiles, profileName);
    }

    private boolean contains(List<String> profiles, String profileName) {
        for (String profile : profiles) {
            if (profileName.equals(profile))
                return true;
        }
        return false;
    }

    public final boolean isCoreEnabled() {
        return corePreparationHandler.isEnabled();
    }

    public final CorePreparationHandler getCorePreparationHandler() {
        return corePreparationHandler;
    }

//    @Override
//    protected void initCHPreparationHandler() {
//        CHPreparationHandler chPreparationHandler = getCHPreparationHandler();
//        if (chPreparationHandler.hasCHConfigs()) {
//            return;
//        }
//
//        for (CHProfile chProfile : chPreparationHandler.getCHProfiles()) {
//            Profile profile = profilesByName.get(chProfile.getProfile());
//            Weighting weighting = createWeighting(profile, new PMap());
//
//            if (profile.getVehicle().equals(FlagEncoderNames.HEAVYVEHICLE)) {
//                HeavyVehicleAttributesGraphStorage hgvStorage = GraphStorageUtils.getGraphExtension(getBaseGraph(), HeavyVehicleAttributesGraphStorage.class);
//                EdgeFilter hgvEdgeFilter = new HeavyVehicleEdgeFilter(HeavyVehicleAttributes.HGV, null, hgvStorage);
//                weighting = new HgvAccessWeighting(weighting, hgvEdgeFilter);
//            }
//
//            if (profile.isTurnCosts()) {
//                chPreparationHandler.addCHConfig(CHConfig.edgeBased(profile.getName(), weighting));
//            } else {
//                chPreparationHandler.addCHConfig(CHConfig.nodeBased(profile.getName(), weighting));
//            }
//        }
//    }

//    protected void loadORS() {
//        List<CHConfig> chConfigs;
//        if (corePreparationHandler.isEnabled()) {
//            initCorePreparationHandler();
//            chConfigs = corePreparationHandler.getCHConfigs();
//        } else {
//            chConfigs = emptyList();
//        }
//
//        if (getBaseGraph() instanceof BaseGraph)
//            ((BaseGraph) getBaseGraph()).addCoreGraphs(chConfigs);
//        else
//            throw new IllegalStateException("Expected an instance of BaseGraph");
//    }

//    private void initCorePreparationHandler() {
//        if (corePreparationHandler.hasCHConfigs()) {
//            return;
//        }
//
//        for (com.graphhopper.config.CHProfile chProfile : corePreparationHandler.getCHProfiles()) {
//            Profile profile = profilesByName.get(chProfile.getProfile());
//            corePreparationHandler.addCHConfig(new CHConfig(profile.getName(), createWeighting(profile, new PMap()), profile.isTurnCosts(), CHConfig.TYPE_CORE));
//        }
//    }

    private void initCoreLMPreparationHandler() {
        if (coreLMPreparationHandler.hasLMProfiles())
            return;

        CoreLMOptions coreLMOptions = coreLMPreparationHandler.getCoreLMOptions();
        coreLMOptions.createRestrictionFilters(getBaseGraph());

        for (LMProfile lmProfile : coreLMPreparationHandler.getLMProfiles()) {
            if (lmProfile.usesOtherPreparation())
                continue;
            Profile profile = profilesByName.get(lmProfile.getProfile());
            Weighting weighting = createWeighting(profile, new PMap(), true);
            for (LMEdgeFilterSequence edgeFilter : coreLMOptions.getFilters()) {
                CoreLMConfig coreLMConfig = new CoreLMConfig(profile.getName(), weighting);
                coreLMConfig.setEdgeFilter(edgeFilter);
                coreLMPreparationHandler.addLMConfig(coreLMConfig);
            }
        }
    }

    protected void prepareCore(boolean closeEarly) {
        for (CHProfile profile : corePreparationHandler.getCHProfiles()) {
            if (!getProfileVersion(profile.getProfile()).isEmpty()
                    && !getProfileVersion(profile.getProfile()).equals("" + profilesByName.get(profile.getProfile()).getVersion()))
                throw new IllegalArgumentException("Core preparation of " + profile.getProfile() + " already exists in storage and doesn't match configuration");
        }
        if (isCoreEnabled()) {
            ensureWriteAccess();
            BaseGraph baseGraph = getBaseGraph();
            baseGraph.freeze();
            corePreparationHandler.prepare(baseGraph, getProperties(), corePreparationHandler.getCHConfigs(), closeEarly);
            getProperties().put(GRParameters.Core.PREPARE + "done", true);
            for (CHProfile profile : corePreparationHandler.getCHProfiles()) {
                // 可能覆盖来自 CH/LM 的已有键
                setProfileVersion(profile.getProfile(), profilesByName.get(profile.getProfile()).getVersion());
            }
        }
    }

    private boolean isCorePrepared() {
        return "true".equals(getProperties().get(GRParameters.Core.PREPARE + "done"))
                // 在 >0.9 版本中移除旧属性
                || "true".equals(getProperties().get("prepare.done"));
    }

    public final boolean isCoreLMEnabled() {
        return coreLMPreparationHandler.isEnabled();
    }

//    /**
//     * For landmarks it is required to always call this method: either it creates the landmark data or it loads it.
//     */
//    protected void loadOrPrepareCoreLM(boolean closeEarly) {
//        for(LMProfile profile : this.coreLMPreparationHandler.getLMProfiles()) {
//            if (!this.getCoreLMProfileVersion(profile.getProfile()).isEmpty()) {
//                String var10000 = this.getLMProfileVersion(profile.getProfile());
//                int var10001 = this.getProfileHash((Profile)this.profilesByName.get(profile.getProfile()));
//                if (!var10000.equals("" + var10001)) {
//                    throw new IllegalArgumentException("LM preparation of " + profile.getProfile() + " already exists in storage and doesn't match configuration");
//                }
//            }
//        }
//
//        List<LMConfig> lmConfigs = this.createCoreLMConfigs(this.lmPreparationHandler.getLMProfiles());
//        List<LandmarkStorage> loaded = this.lmPreparationHandler.load(lmConfigs, this.baseGraph, this.encodingManager);
//        List<LMConfig> loadedConfigs = loaded.stream().map(LandmarkStorage::getLMConfig).toList();
//        List<LMConfig> configsToPrepare = (List)lmConfigs.stream().filter((c) -> !loadedConfigs.contains(c)).collect(Collectors.toList());
//        List<PrepareLandmarks> prepared = this.prepareLM(closeEarly, configsToPrepare);
//        this.landmarks = new LinkedHashMap();
//
//        for(LMProfile lmp : this.lmPreparationHandler.getLMProfiles()) {
//            String prepProfile = lmp.usesOtherPreparation() ? lmp.getPreparationProfile() : lmp.getProfile();
//            Optional<LandmarkStorage> loadedLMS = loaded.stream().filter((lms) -> lms.getLMConfig().getName().equals(prepProfile)).findFirst();
//            Optional<PrepareLandmarks> preparedLMS = prepared.stream().filter((pl) -> pl.getLandmarkStorage().getLMConfig().getName().equals(prepProfile)).findFirst();
//            if (loadedLMS.isPresent() && preparedLMS.isPresent()) {
//                throw new IllegalStateException("LM should be either loaded or prepared, but not both: " + prepProfile);
//            }
//
//            if (preparedLMS.isPresent()) {
//                this.setLMProfileVersion(lmp.getProfile(), this.getProfileHash((Profile)this.profilesByName.get(lmp.getProfile())));
//                this.landmarks.put(lmp.getProfile(), ((PrepareLandmarks)preparedLMS.get()).getLandmarkStorage());
//            } else {
//                loadedLMS.ifPresent((landmarkStorage) -> this.landmarks.put(lmp.getProfile(), landmarkStorage));
//            }
//        }
//    }

    //TODO 重构：这与 RoutingProfile 中的代码重复，可能应移到某个状态维护类中。
    public final boolean isCHAvailable(String profileName) {
        return getCHPreparationHandler().isEnabled() && hasCHProfile(profileName);
    }

    public final boolean isLMAvailable(String profileName) {
        return getLMPreparationHandler().isEnabled() && hasLMProfile(profileName);
    }

    public final boolean isCoreAvailable(String profileName) {
        return getCorePreparationHandler().isEnabled() && hasCoreProfile(profileName);
    }

    public final boolean isFastIsochroneAvailable(RouteSearchContext searchContext, TravelRangeType travelRangeType) {
        return eccentricity != null && eccentricity.isAvailable(GRWeightingFactory.createIsochroneWeighting(searchContext, getEncodingManager()));
    }

    /**
     * 分区
     */
    public final FastIsochroneFactory getFastIsochroneFactory() {
        return fastIsochroneFactory;
    }

    protected void preparePartition() {
        if (fastIsochroneFactory.isEnabled()) {
            ensureWriteAccess();

            getBaseGraph().freeze();
            fastIsochroneFactory.prepare(getProperties());
            getProperties().put(GRParameters.FastIsochrone.PREPARE + "done", true);
        }
    }

    private boolean isPartitionPrepared() {
        return "true".equals(getProperties().get(GRParameters.FastIsochrone.PREPARE + "done"));
    }

//    private void calculateContours() {
//        if (fastIsochroneFactory.getCellStorage().isContourPrepared())
//            return;
//        Contour contour = new Contour(getBaseGraph(), getBaseGraph().getNodeAccess(), fastIsochroneFactory.getIsochroneNodeStorage(), fastIsochroneFactory.getCellStorage());
//        contour.calculateContour();
//    }
//
//    private void calculateCellProperties(Weighting weighting, EdgeFilter edgeFilter, String encodeName, IsochroneNodeStorage isochroneNodeStorage, CellStorage cellStorage) {
//        if (eccentricity == null)
//            eccentricity = new Eccentricity(getBaseGraph(), getLocationIndex(), isochroneNodeStorage, cellStorage);
//        if (!eccentricity.loadExisting(weighting)) {
//            eccentricity.calcEccentricities(weighting, edgeFilter, flagEncoder);
//            eccentricity.calcBorderNodeDistances(weighting, edgeFilter, flagEncoder);
//        }
//    }

    public Eccentricity getEccentricity() {
        return eccentricity;
    }


    public boolean isTrafficEnabled() {
        return GraphStorageUtils.getGraphExtension(getBaseGraph(), TrafficGraphStorage.class) != null;
    }

    public long getMemoryUsage() {
        long mem = 0;
        if (getLMPreparationHandler().isEnabled()) {
            mem += getLMPreparationHandler().getPreparations().stream().mapToLong(lm -> lm.getLandmarkStorage().getCapacity()).sum();
        }
        if (isCoreEnabled()) {
            // core 的 CH 预处理在 BaseGraph.getCapacity() 中处理
            mem += coreLMPreparationHandler.getPreparations().stream().mapToLong(lm -> lm.getLandmarkStorage().getCapacity()).sum();
        }
        if (fastIsochroneFactory.isEnabled()) {
            mem += fastIsochroneFactory.getCapacity();
        }
        return mem + getBaseGraph().getCapacity();
    }

    public EngineProperties getEngineProperties() {
        return engineProperties;
    }

//    private String getCHProfileVersion(String profile) {
//        return this.properties.get("graph.prepare.corelm." + profile + ".version");
//    }
//
//    private void setCHProfileVersion(String profile, int version) {
//        this.properties.put("graph.prepare.corelm." + profile + ".version", version);
//    }
}
