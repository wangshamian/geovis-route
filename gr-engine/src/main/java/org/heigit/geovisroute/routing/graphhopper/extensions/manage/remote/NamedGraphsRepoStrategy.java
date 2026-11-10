package org.heigit.geovisroute.routing.graphhopper.extensions.manage.remote;

import lombok.AllArgsConstructor;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.GraphManagementRuntimeProperties;
import org.heigit.geovisroute.routing.graphhopper.extensions.manage.local.GRGraphFolderStrategy;

@AllArgsConstructor
public class NamedGraphsRepoStrategy implements GRGraphRepoStrategy {

    private final GraphManagementRuntimeProperties managementProperties;

    private String getConcatenatedRepoFileName() {
        return String.join("_",
                managementProperties.getRepoProfileGroup(),
                managementProperties.getRepoCoverage(),
                managementProperties.getGraphVersion(),
                managementProperties.getEncoderName()
        );
    }

    private String getConcatenatedRepoFileName(String extension) {
        return getConcatenatedRepoFileName() + "." + extension;
    }

    @Override
    public String getRepoCompressedGraphFileName() {
        return getConcatenatedRepoFileName(GRGraphFolderStrategy.GRAPH_DOWNLOAD_FILE_EXTENSION);
    }

    @Override
    public String getRepoGraphBuildInfoFileName() {
        return getConcatenatedRepoFileName(GRGraphFolderStrategy.GRAPH_BUILD_INFO_FILE_EXTENSION);
    }
}
