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

import com.graphhopper.GraphHopperConfig;
import com.graphhopper.config.CHProfile;
import com.graphhopper.routing.ev.DefaultImportRegistry;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.StorableProperties;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.config.EngineProperties;
import org.heigit.geovisroute.config.profile.ExecutionProperties;
import org.heigit.geovisroute.config.profile.ProfileProperties;
import org.heigit.geovisroute.routing.graphhopper.extensions.*;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.GRGraphManager;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.builders.BordersGraphStorageBuilder;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.builders.GraphStorageBuilder;
import org.heigit.geovisroute.routing.pathprocessors.GRPathProcessorFactory;
import org.heigit.geovisroute.util.TimeUtility;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 该类生成 {@link RoutingProfile} 类，并被几乎所有服务类使用，例如
 * <p>
 * {@link RoutingProfileManager} 等。
 *
 * @author Openrouteserviceteam
 * @author Julian Psotta, julian@openrouteservice.org
 */
public class RoutingProfile {
    private static final Logger LOGGER = Logger.getLogger(RoutingProfile.class);
    private static final Object lockObj = new Object();
    private static int profileIdentifier = 0;
    private final Integer[] mRoutePrefs;

    private String profileName;
    private ProfileProperties profileProperties;
    private EngineProperties engineProperties;
    private String graphVersion;

    private final GRGraphHopper mGraphHopper;
    private String astarApproximation;
    private Double astarEpsilon;

    public RoutingProfile(String profileName, ProfileProperties profile, EngineProperties engine, String graphVersion, RoutingProfileLoadContext loadCntx) throws Exception {

        this.profileName = profileName;
        this.profileProperties = profile;
        this.engineProperties = engine;
        this.graphVersion = graphVersion;

        mRoutePrefs = profile.getProfilesTypes();
        mGraphHopper = initGraphHopper(loadCntx);
        ExecutionProperties execution = profile.getService().getExecution();

        if (execution.getMethods().getAstar().getApproximation() != null)
            astarApproximation = execution.getMethods().getAstar().getApproximation();
        if (execution.getMethods().getAstar().getEpsilon() != null)
            astarEpsilon = execution.getMethods().getAstar().getEpsilon();
    }


    public GRGraphHopper initGraphHopper(RoutingProfileLoadContext loadCntx) throws Exception {
        GRGraphManager orsGraphManager = GRGraphManager.initializeGraphManagement(graphVersion, engineProperties, profileProperties);
        profileProperties = orsGraphManager.loadProfilePropertiesFromActiveGraph(orsGraphManager, profileProperties);

        GRGraphHopperConfig args = GRGraphHopperConfig.createGHSettings(profileProperties, engineProperties, orsGraphManager.getActiveGraphDirAbsPath());

        int profileId;
        synchronized (lockObj) {
            profileIdentifier++;
            profileId = profileIdentifier;
        }

        long startTime = System.currentTimeMillis();

        GraphProcessContext gpc = new GraphProcessContext(profileProperties);
        gpc.setGetElevationFromPreprocessedData(engineProperties.getElevation().getPreprocessed());

        // GraphHopper 配置也必须传入构造函数：GraphHopperGtfs 会保留对它的引用
        //（例如用于判断需要导入的是 OSM 文件还是 GTFS feed）。
        GRGraphHopper gh = new GRGraphHopper(gpc, engineProperties, profileProperties, args);
        gh.setOrsGraphManager(orsGraphManager);
        DefaultImportRegistry flagEncoderFactory = new DefaultImportRegistry();
        gh.setImportRegistry(flagEncoderFactory);

        GRPathProcessorFactory pathProcessorFactory = new GRPathProcessorFactory();
        gh.setPathProcessorFactory(pathProcessorFactory);

        gh.init(args);

        // MARQ24：确保在多个车辆 profile 中只使用一个 ElevationProvider 实例
        // 这样高程数据的缓存就可以在不同车辆之间复用。[loadCntx 是一个单一
        // 对象，会在（可能存在的）多个运行实例之间共享]
        if (loadCntx.getElevationProvider() != null) {
            if (args.has("graph.elevation.provider")) {
                gh.setElevationProvider(loadCntx.getElevationProvider());
            }
        } else {
            loadCntx.setElevationProvider(gh.getElevationProvider());
        }
        gh.setORSGraphFactory(new GRGraphFactory(gpc.getStorageBuilders()));

        gh.importOrLoad();
        // 保存 CountryBordersReader 以备后用
        for (GraphStorageBuilder builder : gpc.getStorageBuilders()) {
            if (builder.getName().equals(BordersGraphStorageBuilder.BUILDER_NAME)) {
                pathProcessorFactory.setCountryBordersReader(((BordersGraphStorageBuilder) builder).getCbReader());
            }
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[%d] Profile: '%s', encoder: '%s', location: '%s'.".formatted(profileId, profileProperties.getProfileName(), profileProperties.getEncoderName().toString(), gh.getOrsGraphManager().getActiveGraphDirAbsPath()));
            BaseGraph baseGraph = gh.getBaseGraph();
            LOGGER.info("[%d] Edges: %s - Nodes: %s.".formatted(profileId, baseGraph.getEdges(), baseGraph.getNodes()));
            LOGGER.info("[%d] Total time: %s.".formatted(profileId, TimeUtility.getElapsedTime(startTime, true)));
            LOGGER.info("[%d] Finished at: %s.".formatted(profileId, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
        }

        // 生成一个标记，用于帮助跟踪 OSM 文件大小的变化。TODO 检查是否仍在使用
        if (profileProperties.getBuild().getSourceFile() != null) {
            File file = new File(profileProperties.getBuild().getSourceFile().toAbsolutePath().toString());
            Path pathTimestamp = Paths.get(gh.getOrsGraphManager().getActiveGraphDirAbsPath(), "stamp.txt");
            File file2 = pathTimestamp.toFile();
            if (!file2.exists())
                Files.write(pathTimestamp, Long.toString(file.length()).getBytes());
        }
        return gh;
    }

    public boolean hasCHProfile(String profileName) {
        boolean hasCHProfile = false;
        for (CHProfile chProfile : getGraphhopper().getCHPreparationHandler().getCHProfiles()) {
            if (profileName.equals(chProfile.getProfile()))
                hasCHProfile = true;
        }
        return hasCHProfile;
    }

    public long getMemoryUsage() {
        return mGraphHopper.getMemoryUsage();
    }

    public GRGraphHopper getGraphhopper() {
        return mGraphHopper;
    }

    public StorableProperties getGraphProperties() {
        return mGraphHopper.getProperties();
    }

    public ProfileProperties getProfileConfiguration() {
        return profileProperties;
    }

    public void close() {
        mGraphHopper.close();
    }

    public String getAstarApproximation() {
        return astarApproximation;
    }

    public Double getAstarEpsilon() {
        return astarEpsilon;
    }

    public boolean equals(Object o) {
        return o != null && o.getClass().equals(RoutingProfile.class) && this.hashCode() == o.hashCode();
    }

    public int hashCode() {
        return mGraphHopper.getBaseGraph().getDirectory().getLocation().hashCode();
    }

    public String name() {
        return this.profileName;
    }

    public ProfileProperties getProfileProperties() {
        return this.profileProperties;
    }
}
