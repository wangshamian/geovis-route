package org.heigit.geovisroute.routing.graphhopper.extensions.manage.remote;

public class NullGraphRepoClient implements GRGraphRepoClient {
    @Override
    public void downloadGraphIfNecessary() {
        // The NullGraphRepoClient does nothing at this point.
    }
}
