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
package org.heigit.geovisroute.routing.graphhopper.extensions.core;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.lm.LMConfig;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.lm.PrepareLandmarks;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.Directory;
import com.graphhopper.util.PMap;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraph;
import org.heigit.geovisroute.routing.graphhopper.extensions.edgefilters.core.LMEdgeFilterSequence;

/**
 * 该类在核心区内执行 ALT 算法（A*、地标、三角不等式）的预处理。
 * <p>
 * http://www.siam.org/meetings/alenex05/papers/03agoldberg.pdf
 * <p>
 * 本代码基于 GraphHopper GmbH 的代码。
 *
 * @author Peter Karich
 * @author Andrzej Oles
 */
public class PrepareCoreLandmarks extends PrepareLandmarks {
    private final LMEdgeFilterSequence landmarksFilter;

    public PrepareCoreLandmarks(Directory dir, BaseGraph baseGraph, EncodedValueLookup encodedValueLookup, CoreLMConfig lmConfig, int landmarks) {
        super(dir, baseGraph, encodedValueLookup, lmConfig, landmarks);
        this.landmarksFilter = lmConfig.getEdgeFilter();
    }

    public LandmarkStorage createLandmarkStorage(Directory dir, BaseGraph baseGraph, EncodingManager encodingManager, LMConfig lmConfig, int landmarks) {
        if (!(lmConfig instanceof CoreLMConfig))
            throw (new IllegalStateException("Expected instance of CoreLMConfig"));
        if (!(graph instanceof GRGraph))
            throw (new IllegalStateException("Expected instance of GRGraphHopperStorage"));

        return new CoreLandmarkStorage(dir, (GRGraph) baseGraph, encodingManager, (CoreLMConfig) lmConfig, landmarks);
    }

    public boolean matchesFilter(PMap pmap) {
        //如果地标集合用于可规避项则返回 true。
        //如果查询没有可规避项且集合也没有可规避项，同样返回 true
        return landmarksFilter.isFilter(pmap);
    }
}
