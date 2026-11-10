package org.heigit.geovisroute.routing.pathprocessors;

import com.graphhopper.routing.util.PathProcessor;
import com.graphhopper.routing.util.PathProcessorFactory;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.util.PMap;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.routing.graphhopper.extensions.reader.borders.CountryBordersReader;

public class GRPathProcessorFactory implements PathProcessorFactory {
    private static final Logger LOGGER = Logger.getLogger(GRPathProcessorFactory.class.getName());
    private CountryBordersReader countryBordersReader;

    @Override
    public PathProcessor createPathProcessor(PMap opts, BaseGraph baseGraph, StorableProperties storableProperties) {
        try {
            if (countryBordersReader != null) {
                return new ExtraInfoProcessor(opts, baseGraph, storableProperties, countryBordersReader);
            }
            return new ExtraInfoProcessor(opts, baseGraph, storableProperties);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return PathProcessor.DEFAULT;
    }

    public void setCountryBordersReader(CountryBordersReader cbr) {
        this.countryBordersReader = cbr;
    }
}
