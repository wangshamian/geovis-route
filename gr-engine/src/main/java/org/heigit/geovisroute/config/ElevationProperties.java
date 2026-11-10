package org.heigit.geovisroute.config;

import lombok.Getter;
import lombok.Setter;
import org.heigit.geovisroute.common.DataAccessEnum;

import java.nio.file.Path;

@Getter
@Setter
public class ElevationProperties {
    private Boolean preprocessed;
    private DataAccessEnum dataAccess;
    private Boolean cacheClear;
    private String provider;
    private Path cachePath;

    public ElevationProperties() {
    }

    public ElevationProperties(String ignored) {
        // 该构造函数用于创建一个空对象，以便在 JSON 序列化时忽略它。
    }

    public Path getCachePath() {
        return cachePath != null ? cachePath.toAbsolutePath() : null;
    }
}