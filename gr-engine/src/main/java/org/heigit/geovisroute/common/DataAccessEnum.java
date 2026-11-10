package org.heigit.geovisroute.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DataAccessEnum {
    RAM_STORE("RAM_STORE"),
    MMAP("MMAP"),
    MMAP_RO("MMAP_RO");

    private final String store;

    DataAccessEnum(String store) {
        this.store = store;
    }

    @JsonValue
    public String getStore() {
        return store;
    }
}
