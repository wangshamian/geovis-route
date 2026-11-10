package org.heigit.geovisroute.routing.graphhopper.extensions.manage;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.config.EngineProperties;
import org.heigit.geovisroute.config.profile.ProfileProperties;
import org.heigit.geovisroute.exceptions.GRGraphFileManagerException;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraphHopper;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.local.FlatORSGraphFolderStrategy;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.local.GRGraphFileManager;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.local.GRGraphFolderStrategy;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.remote.*;

import java.io.File;

import static java.util.Optional.ofNullable;

@NoArgsConstructor
@AllArgsConstructor
public class GRGraphManager {

    private static final Logger LOGGER = Logger.getLogger(GRGraphManager.class.getName());
    public static final String UPDATE_LOCKFILE_NAME = "update.lock";
    public static final String ACTIVATION_LOCKFILE_NAME = "activation.lock";

    private GraphManagementRuntimeProperties managementRuntimeProperties;
    private GRGraphFileManager orsGraphFileManager;
    private GRGraphRepoClient orsGraphRepoClient;

    public static GRGraphManager initializeGraphManagement(String graphVersion, EngineProperties engineProperties, ProfileProperties profileProperties) {
        GraphManagementRuntimeProperties managementProps = GraphManagementRuntimeProperties.Builder.from(engineProperties, profileProperties, graphVersion).build();
        return initializeGraphManagement(managementProps);
    }

    public static GRGraphManager initializeGraphManagement(GraphManagementRuntimeProperties managementProps) {
        GRGraphFolderStrategy orsGraphFolderStrategy = new FlatORSGraphFolderStrategy(managementProps);
        GRGraphRepoStrategy orsGraphRepoStrategy = new NamedGraphsRepoStrategy(managementProps);
        GRGraphFileManager orsGraphFileManager = new GRGraphFileManager(managementProps, orsGraphFolderStrategy);
        orsGraphFileManager.initialize();

        GRGraphRepoClient orsGraphRepoClient = getOrsGraphRepoClient(managementProps, orsGraphRepoStrategy, orsGraphFileManager);

        GRGraphManager orsGraphManager = new GRGraphManager(managementProps, orsGraphFileManager, orsGraphRepoClient);
        orsGraphManager.manageStartup();
        return orsGraphManager;
    }

    public static GRGraphRepoClient getOrsGraphRepoClient(GraphManagementRuntimeProperties managementProps, GRGraphRepoStrategy orsGraphRepoStrategy, GRGraphFileManager orsGraphFileManager) {
        GRGraphRepoClient orsGraphRepoClient = new NullGraphRepoClient();

        switch (managementProps.getDerivedRepoType()) {
            case HTTP -> {
                LOGGER.debug("Using HttpGraphRepoClient for repoUrl %s".formatted(managementProps.getDerivedRepoBaseUrl()));
                orsGraphRepoClient = new HttpGraphRepoClient(managementProps, orsGraphRepoStrategy, orsGraphFileManager);
            }
            case FILESYSTEM -> {
                LOGGER.debug("Using FileSystemGraphRepoClient for repoUri %s".formatted(managementProps.getDerivedRepoPath()));
                orsGraphRepoClient = new FileSystemGraphRepoClient(managementProps, orsGraphRepoStrategy, orsGraphFileManager);
            }
            case MINIO -> {
                LOGGER.debug("Using MinioGraphRepoClient for repoUrl %s".formatted(managementProps.getDerivedRepoBaseUrl()));
                orsGraphRepoClient = new MinioGraphRepoClient(managementProps, orsGraphRepoStrategy, orsGraphFileManager);
            }
            case NULL -> {
                LOGGER.debug("No valid repositoryUri configured, using NullGraphRepoClient.");
                orsGraphRepoClient = new NullGraphRepoClient();
            }
        }

        return orsGraphRepoClient;
    }

    public ProfileProperties loadProfilePropertiesFromActiveGraph(GRGraphManager orsGraphManager, ProfileProperties profileProperties) throws GRGraphFileManagerException {
        profileProperties.mergeLoaded(orsGraphManager.getActiveGraphProfileProperties());
        return profileProperties;
    }

    public String getProfileName() {
        return managementRuntimeProperties.getLocalProfileName();
    }

    public String getQualifiedProfileName() {
        return orsGraphFileManager.getProfileDescriptiveName();
    }

    public String getActiveGraphDirAbsPath() {
        return orsGraphFileManager.getActiveGraphDirAbsPath();
    }

    public boolean isBusy() {
        return orsGraphFileManager.isBusy();
    }

    public boolean hasGraphDownloadFile() {
        return orsGraphFileManager.hasGraphDownloadFile();
    }

    public boolean hasDownloadedExtractedGraph() {
        return orsGraphFileManager.hasDownloadedExtractedGraph();
    }

    public boolean useGraphRepository() {
        if (managementRuntimeProperties == null) return false;
        if (!managementRuntimeProperties.isEnabled()) return false;
        if (StringUtils.isBlank(managementRuntimeProperties.getRepoName())) return false;

        return managementRuntimeProperties.getDerivedRepoType() != GraphManagementRuntimeProperties.GraphRepoType.NULL;
    }

    public void manageStartup() {
        if (!useGraphRepository()) return;

        orsGraphFileManager.cleanupIncompleteFiles();

        boolean hasActiveGraph = orsGraphFileManager.hasActiveGraph();
        boolean hasDownloadedExtractedGraph = orsGraphFileManager.hasDownloadedExtractedGraph();

        if (!hasActiveGraph && !hasDownloadedExtractedGraph && useGraphRepository()) {
            LOGGER.debug("[%s] No local graph or extracted downloaded graph found - trying to download and extract graph from repository".formatted(getQualifiedProfileName()));
            downloadAndExtractLatestGraphIfNecessary();
            orsGraphFileManager.activateExtractedDownloadedGraph();
        }
        if (!hasActiveGraph && hasDownloadedExtractedGraph) {
            LOGGER.debug("[%s] Found extracted downloaded graph only".formatted(getQualifiedProfileName()));
            orsGraphFileManager.activateExtractedDownloadedGraph();
        }
        if (hasActiveGraph && hasDownloadedExtractedGraph) {
            LOGGER.debug("[%s] Found local graph and extracted downloaded graph".formatted(getQualifiedProfileName()));
            orsGraphFileManager.backupExistingGraph();
            orsGraphFileManager.activateExtractedDownloadedGraph();
        }
        if (hasActiveGraph && !hasDownloadedExtractedGraph) {
            LOGGER.debug("[%s] Found local graph only".formatted(getQualifiedProfileName()));
        }
    }

    public void downloadAndExtractLatestGraphIfNecessary() {
        if (!useGraphRepository()) return;
        if (orsGraphFileManager.isBusy()) {
            LOGGER.debug("[%s] GRGraphManager is busy - skipping download".formatted(getQualifiedProfileName()));
            return;
        }
        orsGraphRepoClient.downloadGraphIfNecessary();
        orsGraphFileManager.extractDownloadedGraph();
    }

    public boolean hasUpdateLock() {
        File restartLockFile = new File(orsGraphFileManager.getGraphsRootDirAbsPath() + File.separator + UPDATE_LOCKFILE_NAME);
        return restartLockFile.exists();
    }

    public boolean hasActivationLock() {
        File restartLockFile = new File(orsGraphFileManager.getGraphsRootDirAbsPath() + File.separator + ACTIVATION_LOCKFILE_NAME);
        return restartLockFile.exists();
    }

    public void writeOrsGraphBuildInfoFileIfNotExists(GRGraphHopper gh) {
        orsGraphFileManager.writeOrsGraphBuildInfoFileIfNotExists(gh);
    }

    public GraphBuildInfo getActiveGraphBuildInfo() throws GRGraphFileManagerException {
        return orsGraphFileManager.getActiveGraphBuildInfo();
    }

    public ProfileProperties getActiveGraphProfileProperties() throws GRGraphFileManagerException {
        return ofNullable(getActiveGraphBuildInfo())
                .map(GraphBuildInfo::getPersistedGraphBuildInfo)
                .map(PersistedGraphBuildInfo::getProfileProperties)
                .orElse(null);
    }
}
