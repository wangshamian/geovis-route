package com.graphhopper.routing;

import com.graphhopper.routing.util.DefaultPathProcessor;
import com.graphhopper.routing.util.PathProcessor;
import com.graphhopper.routing.util.PathProcessorFactory;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.util.PMap;

// ORS-GH MOD
public class DefaultPathProcessorFactory implements PathProcessorFactory {

    @Override
    public PathProcessor createPathProcessor(PMap opts, BaseGraph baseGraph, StorableProperties storableProperties) {
        return new DefaultPathProcessor();
    }
}
