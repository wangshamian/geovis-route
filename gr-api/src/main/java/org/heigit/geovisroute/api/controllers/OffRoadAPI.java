package org.heigit.geovisroute.api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.GHRequest;
import com.graphhopper.OffRoadResponse;
import com.graphhopper.offroad.OffRoader;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.PointList;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.shapes.GHPoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.heigit.geovisroute.api.config.ApiEngineProperties;
import org.heigit.geovisroute.routing.RoutingProfile;
import org.heigit.geovisroute.routing.RoutingProfileManager;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraphHopperConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 越野路径接口，基于 gr-engine 的 OffRoader 网格坡度分析实现。
 */
@RestController
@Tag(name = "Off Road Service", description = "基于网格坡度分析的越野路径规划")
@RequestMapping("/v2/offroad")
public class OffRoadAPI {

    private static final double DEFAULT_GRID_INTERVAL = 300;
    private static final double MIN_GRID_INTERVAL = 10;
    private static final double MAX_GRID_INTERVAL = 1000;
    private static final double DEFAULT_MAX_SLOPE = 20;
    private static final double MIN_MAX_SLOPE = 1;
    private static final double MAX_MAX_SLOPE = 60;
    /**
     * 越野交通工具及其平均通行速度（km/h），用于估算耗时。
     */
    private static final Map<String, Double> VEHICLE_SPEEDS_KMH = Map.of(
            "offroad-car", 20.0,
            "offroad-truck", 15.0,
            "motorcycle", 35.0,
            "bicycle", 12.0,
            "foot", 5.0);
    private static final String DEFAULT_VEHICLE = "offroad-car";
    /**
     * 高程剖面最多返回的采样点数量。
     */
    private static final int MAX_PROFILE_SAMPLES = 128;

    private final ApiEngineProperties apiEngineProperties;
    private volatile ElevationProvider elevationProvider;
    /**
     * SRTM 高程数据源在创建瓦片 DataAccess 时不是线程安全的，
     * 多个越野请求并发访问同一瓦片会抛 "DataAccess ... has already been created"，这里串行化计算。
     */
    private final Object offroadLock = new Object();

    public OffRoadAPI(ApiEngineProperties apiEngineProperties) {
        this.apiEngineProperties = apiEngineProperties;
    }

    @PostMapping(produces = "application/json;charset=UTF-8")
    @Operation(description = "根据起点和终点计算越野路径，返回路径几何、统计信息与坡度超限的障碍区域", summary = "Off Road Service")
    public ResponseEntity<Object> offroad(@RequestBody OffRoadRequest request) {
        if (request.coordinates() == null || request.coordinates().size() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "coordinates 至少需要起点和终点两个坐标"));
        }

        List<GHPoint> points = new ArrayList<>();
        for (List<Double> coordinate : request.coordinates()) {
            if (coordinate == null || coordinate.size() < 2) {
                return ResponseEntity.badRequest().body(Map.of("error", "坐标格式应为 [经度, 纬度]"));
            }
            Double lon = coordinate.get(0);
            Double lat = coordinate.get(1);
            if (lon == null || lat == null || lon.isNaN() || lat.isNaN()) {
                return ResponseEntity.badRequest().body(Map.of("error", "坐标不能为空或 NaN"));
            }
            if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                return ResponseEntity.badRequest().body(Map.of("error", "坐标超出范围：经度需在 -180~180，纬度需在 -90~90"));
            }
            points.add(new GHPoint(lat, lon));
        }

        double gridInterval = request.gridInterval() == null ? DEFAULT_GRID_INTERVAL : request.gridInterval();
        if (gridInterval < MIN_GRID_INTERVAL || gridInterval > MAX_GRID_INTERVAL) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    String.format("grid_interval 需在 %.0f~%.0f 米之间", MIN_GRID_INTERVAL, MAX_GRID_INTERVAL)));
        }
        double maxSlope = request.maxSlope() == null ? DEFAULT_MAX_SLOPE : request.maxSlope();
        if (maxSlope < MIN_MAX_SLOPE || maxSlope > MAX_MAX_SLOPE) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    String.format("max_slope 需在 %.0f~%.0f 度之间", MIN_MAX_SLOPE, MAX_MAX_SLOPE)));
        }
        String vehicle = request.vehicle() == null || request.vehicle().isBlank()
                ? DEFAULT_VEHICLE : request.vehicle().trim();
        Double speedKmh = VEHICLE_SPEEDS_KMH.get(vehicle);
        if (speedKmh == null) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "不支持的 vehicle：" + vehicle + "，可选值：" + String.join(", ", VEHICLE_SPEEDS_KMH.keySet())));
        }

        GHRequest ghRequest = new GHRequest(points);
        ghRequest.putHint("grid_interval", gridInterval);
        ghRequest.putHint("max_slope", maxSlope);

        ElevationProvider elevationProvider = resolveElevationProvider();
        OffRoadResponse offRoadResponse;
        try {
            synchronized (offroadLock) {
                offRoadResponse = new OffRoader(elevationProvider).offroad(ghRequest);
            }
        } catch (IllegalArgumentException e) {
            // 网格数量超限、计算超时等参数相关问题统一返回 400
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        }

        PointList pointList = offRoadResponse.getOffRoadPoints();
        List<double[]> coordinates = new ArrayList<>();
        if (pointList != null) {
            // OffRoader 已按起点到终点输出，无需再翻转
            for (int i = 0; i < pointList.size(); i++) {
                coordinates.add(new double[]{pointList.getLon(i), pointList.getLat(i)});
            }
        }

        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "LineString");
        geometry.put("coordinates", coordinates);

        double distance = calcDistance(coordinates);
        Map<String, Object> elevation;
        synchronized (offroadLock) {
            // 高程采样同样访问 SRTM 数据源，必须与路径计算串行
            elevation = buildElevationProfile(elevationProvider, coordinates);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("geometry", geometry);
        result.put("grid_interval", gridInterval);
        result.put("max_slope", maxSlope);
        result.put("vehicle", vehicle);
        result.put("speed_kmh", speedKmh);
        result.put("point_count", coordinates.size());
        result.put("summary", buildSummary(distance, elevation, offRoadResponse.getBarrierCount(), speedKmh));
        if (coordinates.isEmpty()) {
            // 无可行路径时给出可操作的提示，便于前端直接展示
            result.put("message", "未找到满足最大坡度限制的可行路径，请提高 max_slope、增大 grid_interval 或调整起终点位置");
        }
        if (elevation != null) {
            result.put("elevation", elevation);
        }

        JsonFeature barrierFeature = offRoadResponse.getBarrierFeature();
        if (barrierFeature != null && barrierFeature.getGeometry() != null) {
            result.put("barriers", barrierFeature.getGeometry());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 汇总路径长度、预计耗时、坡度与障碍数量等统计信息。
     */
    private Map<String, Object> buildSummary(double distance, Map<String, Object> elevation, int barrierCount, double speedKmh) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("distance", round1(distance));
        // 按所选交通工具的平均速度估算耗时（秒）
        summary.put("duration", Math.round(distance / (speedKmh / 3.6)));
        summary.put("max_slope", elevation == null ? null : elevation.get("max_slope"));
        summary.put("avg_slope", elevation == null ? null : elevation.get("avg_slope"));
        summary.put("ascend", elevation == null ? null : elevation.get("ascend"));
        summary.put("descend", elevation == null ? null : elevation.get("descend"));
        summary.put("barrier_count", barrierCount);
        return summary;
    }

    /**
     * 路径总长度（米）。
     */
    private double calcDistance(List<double[]> coordinates) {
        double total = 0;
        for (int i = 1; i < coordinates.size(); i++) {
            double[] prev = coordinates.get(i - 1);
            double[] cur = coordinates.get(i);
            total += DistanceCalcEarth.DIST_EARTH.calcDist(prev[1], prev[0], cur[1], cur[0]);
        }
        return total;
    }

    /**
     * 沿路径等距重采样，避免路径被简化成极少折点时高程剖面过于粗糙。
     * 采样间距不小于 30 米，最多返回 MAX_PROFILE_SAMPLES 个点。
     */
    private List<double[]> resamplePath(List<double[]> coordinates, int maxSamples) {
        int count = coordinates.size();
        if (count < 2) {
            return coordinates;
        }
        double[] cumulative = new double[count];
        for (int i = 1; i < count; i++) {
            cumulative[i] = cumulative[i - 1] + DistanceCalcEarth.DIST_EARTH.calcDist(
                    coordinates.get(i - 1)[1], coordinates.get(i - 1)[0],
                    coordinates.get(i)[1], coordinates.get(i)[0]);
        }
        double total = cumulative[count - 1];
        if (total <= 0) {
            return coordinates;
        }
        int stations = Math.min(maxSamples, Math.max(2, (int) Math.round(total / 30)));
        List<double[]> result = new ArrayList<>(stations);
        int segment = 0;
        for (int k = 0; k < stations; k++) {
            double target = total * k / (stations - 1);
            while (segment < count - 2 && cumulative[segment + 1] < target) {
                segment++;
            }
            double segmentLength = cumulative[segment + 1] - cumulative[segment];
            double ratio = segmentLength > 0 ? (target - cumulative[segment]) / segmentLength : 0;
            double lon = coordinates.get(segment)[0]
                    + (coordinates.get(segment + 1)[0] - coordinates.get(segment)[0]) * ratio;
            double lat = coordinates.get(segment)[1]
                    + (coordinates.get(segment + 1)[1] - coordinates.get(segment)[1]) * ratio;
            result.add(new double[]{lon, lat});
        }
        return result;
    }

    /**
     * 沿路径采样高程，返回高程剖面（最多 128 个采样点），同时统计最大/平均坡度。
     */
    private Map<String, Object> buildElevationProfile(ElevationProvider provider, List<double[]> coordinates) {
        if (provider == null || provider == ElevationProvider.NOOP || coordinates.size() < 2) {
            return null;
        }
        List<double[]> stations = resamplePath(coordinates, MAX_PROFILE_SAMPLES);
        List<Map<String, Object>> samples = new ArrayList<>();
        double total = 0, ascend = 0, descend = 0;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        double maxSlope = 0, weightedSlope = 0, slopeDistance = 0;
        double prevLat = 0, prevLon = 0, prevEle = Double.NaN;
        for (int i = 0; i < stations.size(); i++) {
            double lon = stations.get(i)[0];
            double lat = stations.get(i)[1];
            double ele = provider.getEle(lat, lon);
            if (Double.isNaN(ele)) {
                continue;
            }
            if (!Double.isNaN(prevEle)) {
                double legDistance = DistanceCalcEarth.DIST_EARTH.calcDist(prevLat, prevLon, lat, lon);
                total += legDistance;
                double diff = ele - prevEle;
                if (diff > 0) {
                    ascend += diff;
                } else {
                    descend -= diff;
                }
                // 相邻采样点间隔过小时坡度噪声较大，只统计有意义的距离
                if (legDistance > 0.5) {
                    double slope = Math.toDegrees(Math.atan(Math.abs(diff) / legDistance));
                    maxSlope = Math.max(maxSlope, slope);
                    weightedSlope += slope * legDistance;
                    slopeDistance += legDistance;
                }
            }
            min = Math.min(min, ele);
            max = Math.max(max, ele);
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("distance", round1(total));
            sample.put("ele", round1(ele));
            // 返回原始 WGS84 坐标，供前端剖面 hover 联动地图
            sample.put("lon", round6(lon));
            sample.put("lat", round6(lat));
            samples.add(sample);
            prevLat = lat;
            prevLon = lon;
            prevEle = ele;
        }
        if (samples.isEmpty()) {
            return null;
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("distance", round1(total));
        profile.put("min", round1(min));
        profile.put("max", round1(max));
        profile.put("ascend", round1(ascend));
        profile.put("descend", round1(descend));
        profile.put("max_slope", round1(maxSlope));
        profile.put("avg_slope", round1(slopeDistance > 0 ? weightedSlope / slopeDistance : 0));
        profile.put("samples", samples);
        return profile;
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private double round6(double value) {
        return Math.round(value * 1_000_000) / 1_000_000.0;
    }

    private ElevationProvider resolveElevationProvider() {
        try {
            List<RoutingProfile> profiles = RoutingProfileManager.getInstance().getUniqueProfiles();
            if (!profiles.isEmpty()) {
                ElevationProvider profileProvider = profiles.get(0).getGraphhopper().getElevationProvider();
                // 路网图为 3D 时直接复用其高程数据源
                if (profileProvider != null && profileProvider != ElevationProvider.NOOP) {
                    return profileProvider;
                }
            }
        } catch (UnsupportedOperationException ignored) {
            // RoutingProfileManager 未初始化时使用独立配置的高程数据源
        }
        return getElevationProvider();
    }

    /**
     * 越野路径不需要把高程写入路网图，因此按 ors.engine.elevation 单独创建并缓存数据源。
     */
    private ElevationProvider getElevationProvider() {
        ElevationProvider provider = elevationProvider;
        if (provider == null) {
            synchronized (this) {
                provider = elevationProvider;
                if (provider == null) {
                    provider = GRGraphHopperConfig.createElevationProvider(apiEngineProperties.getElevation());
                    elevationProvider = provider;
                }
            }
        }
        return provider;
    }

    public record OffRoadRequest(List<List<Double>> coordinates,
                                 @JsonProperty("grid_interval") Double gridInterval,
                                 @JsonProperty("max_slope") Double maxSlope,
                                 @JsonProperty("vehicle") String vehicle) {
    }
}
