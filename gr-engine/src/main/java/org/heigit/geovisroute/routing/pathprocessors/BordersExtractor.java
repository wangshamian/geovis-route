package org.heigit.geovisroute.routing.pathprocessors;

import org.heigit.geovisroute.routing.graphhopper.extensions.storages.BordersGraphStorage;

import java.util.List;

public class BordersExtractor {
    public enum Avoid {CONTROLLED, NONE, ALL}

    private final BordersGraphStorage storage;
    private final int[] avoidCountries;

    public BordersExtractor(BordersGraphStorage storage, int[] avoidCountries) {
        this.storage = storage;
        this.avoidCountries = avoidCountries;
    }

    public int getValue(int edgeId) {
        // 获取边界类型
        return storage.getEdgeValue(edgeId, BordersGraphStorage.Property.TYPE);
    }

    public boolean isBorder(int edgeId) {
        int type = storage.getEdgeValue(edgeId, BordersGraphStorage.Property.TYPE);
        return (type == BordersGraphStorage.OPEN_BORDER || type == BordersGraphStorage.CONTROLLED_BORDER);
    }

    public boolean isControlledBorder(int edgeId) {
        return storage.getEdgeValue(edgeId, BordersGraphStorage.Property.TYPE) == BordersGraphStorage.CONTROLLED_BORDER;
    }

    public boolean isOpenBorder(int edgeId) {
        return storage.getEdgeValue(edgeId, BordersGraphStorage.Property.TYPE) == BordersGraphStorage.OPEN_BORDER;
    }

    public boolean restrictedCountry(int edgeId) {
        int startCountry = storage.getEdgeValue(edgeId, BordersGraphStorage.Property.START);
        int endCountry = storage.getEdgeValue(edgeId, BordersGraphStorage.Property.END);

        for (int i = 0; i < avoidCountries.length; i++) {
            if (startCountry == avoidCountries[i] || endCountry == avoidCountries[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查一组边的起点 node 和终点 node 是否位于同一个国家。
     *
     * @param edgeIds 需要检查所属国家的边
     * @return 若至少有一个 node 位于同一个国家，则返回 true
     */
    public boolean isSameCountry(List<Integer> edgeIds) {
        if (edgeIds.isEmpty())
            return true;

        short country0 = storage.getEdgeValue(edgeIds.get(0), BordersGraphStorage.Property.START);
        short country1 = storage.getEdgeValue(edgeIds.get(0), BordersGraphStorage.Property.END);
        for (int edgeId : edgeIds) {
            short country2 = storage.getEdgeValue(edgeId, BordersGraphStorage.Property.START);
            short country3 = storage.getEdgeValue(edgeId, BordersGraphStorage.Property.END);
            if (country0 != country2
                    && country0 != country3
                    && country1 != country2
                    && country1 != country3)
                return false;
        }
        return true;
    }
}
