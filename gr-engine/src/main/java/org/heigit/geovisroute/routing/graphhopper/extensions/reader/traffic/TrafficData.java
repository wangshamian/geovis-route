package org.heigit.geovisroute.routing.graphhopper.extensions.reader.traffic;

import com.carrotsearch.hppc.IntObjectHashMap;

public class TrafficData {

    private final IntObjectHashMap<TrafficLink> links;
    private final IntObjectHashMap<TrafficPattern> patterns;

    public TrafficData() {
        this.links = new IntObjectHashMap<>();
        this.patterns = new IntObjectHashMap<>();
    }


    /**
     * 向交通数据中添加一个 {@link TrafficLink} 并更新范围。
     *
     * @param link 向交通数据中添加一个 link。
     */
    public void setLink(TrafficLink link) {
        this.links.put(link.getLinkId(), link);
    }

    public IntObjectHashMap<TrafficLink> getLinks() {
        return links;
    }

    public boolean hasLink(Integer linkId) {
        return links.get(linkId) != null;
    }

    public TrafficLink getLink(int linkId) {
        return links.get(linkId);
    }

    public IntObjectHashMap<TrafficPattern> getPatterns() {
        return patterns;
    }

    public void setPattern(TrafficPattern pattern) {
        this.patterns.put(pattern.getPatternId(), pattern);
    }
}
