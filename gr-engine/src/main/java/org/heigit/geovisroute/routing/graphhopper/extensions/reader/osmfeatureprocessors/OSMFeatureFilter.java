package org.heigit.geovisroute.routing.graphhopper.extensions.reader.osmfeatureprocessors;

import com.graphhopper.reader.ReaderElement;

import java.io.InvalidObjectException;

public interface OSMFeatureFilter {

    /**
     * Assign a feature to this filter for processing
     *
     * @param feature
     * @throws InvalidObjectException
     */
    void assignFeatureForFiltering(ReaderElement feature) throws InvalidObjectException;

    /**
     * 已分配给该过滤器的要素是否应被接受到图中
     *
     * @return
     */
    boolean accept();

    /**
     * 为处理准备好已分配给该过滤器的要素
     *
     * @return
     */
    ReaderElement prepareForProcessing();

    /**
     * 该过滤器上的要素是否已处理完毕。
     *
     * @return
     */
    boolean isWayProcessingComplete();

}
