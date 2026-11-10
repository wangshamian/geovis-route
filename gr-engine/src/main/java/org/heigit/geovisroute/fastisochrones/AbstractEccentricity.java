package org.heigit.geovisroute.fastisochrones;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.CellStorage;
import org.heigit.geovisroute.fastisochrones.partitioning.storage.IsochroneNodeStorage;
import org.heigit.geovisroute.fastisochrones.storage.BorderNodeDistanceStorage;
import org.heigit.geovisroute.fastisochrones.storage.EccentricityStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * 离心率计算的父类。存储并排列对离心率和边界节点距离存储的引用。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public abstract class AbstractEccentricity {
    protected BaseGraph baseGraph;
    protected IsochroneNodeStorage isochroneNodeStorage;
    protected CellStorage cellStorage;
    protected List<EccentricityStorage> eccentricityStorages = new ArrayList<>();
    protected List<BorderNodeDistanceStorage> borderNodeDistanceStorages = new ArrayList<>();

    protected AbstractEccentricity(BaseGraph baseGraph) {
        this.baseGraph = baseGraph;
    }

    public abstract void calcEccentricities(Weighting weighting, EdgeFilter additionalEdgeFilter, AbstractAccessParser accessParser);

    public EccentricityStorage getEccentricityStorage(Weighting weighting) {
        if (eccentricityStorages.isEmpty())
            return null;
        for (EccentricityStorage ecc : eccentricityStorages) {
            if (ecc.hasWeighting(weighting))
                return ecc;
        }
        return null;
    }

    public BorderNodeDistanceStorage getBorderNodeDistanceStorage(Weighting weighting) {
        if (borderNodeDistanceStorages.isEmpty())
            return null;
        for (BorderNodeDistanceStorage bnds : borderNodeDistanceStorages) {
            if (bnds.hasWeighting(weighting))
                return bnds;
        }
        return null;
    }

    public boolean loadExisting(Weighting weighting) {
        EccentricityStorage eccentricityStorage = new EccentricityStorage(baseGraph.getDirectory(), weighting, isochroneNodeStorage, baseGraph.getNodes());
        eccentricityStorages.add(eccentricityStorage);

        BorderNodeDistanceStorage borderNodeDistanceStorage = new BorderNodeDistanceStorage(baseGraph.getDirectory(), weighting, isochroneNodeStorage, baseGraph.getNodes());
        borderNodeDistanceStorages.add(borderNodeDistanceStorage);
        borderNodeDistanceStorage.loadExisting();

        return eccentricityStorage.loadExisting();
    }

    public boolean isAvailable(Weighting weighting) {
        return getEccentricityStorage(weighting) != null;
    }
}
