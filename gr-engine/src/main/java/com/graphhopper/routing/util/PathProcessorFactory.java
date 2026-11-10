package com.graphhopper.routing.util;

import com.graphhopper.routing.DefaultPathProcessorFactory;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.util.PMap;

// ORS-GH MOD - new class
public interface PathProcessorFactory {
    PathProcessorFactory DEFAULT = new DefaultPathProcessorFactory();

    PathProcessor createPathProcessor(PMap opts, BaseGraph baseGraph, StorableProperties storableProperties);
}
