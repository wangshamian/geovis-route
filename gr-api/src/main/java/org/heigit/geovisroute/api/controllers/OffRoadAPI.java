package org.heigit.geovisroute.api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.GHRequest;
import com.graphhopper.OffRoadResponse;
import com.graphhopper.offroad.OffRoader;
import com.graphhopper.offroad.RoadAccessProvider;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.PointList;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.shapes.GHPoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.heigit.geovisroute.api.config.ApiEngineProperties;
import org.heigit.geovisroute.common.EncoderNameEnum;
import org.heigit.geovisroute.routing.RoutingProfile;
import org.heigit.geovisroute.routing.RoutingProfileManager;
import org.heigit.geovisroute.routing.graphhopper.extensions.GRGraphHopperConfig;
import org.heigit.geovisroute.routing.graphhopper.extensions.offroad.GraphRoadAccessProvider;
import org.heigit.geovisroute.routing.graphhopper.extensions.parsers.FlagEncoderNames;
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
@RequestMapping("/v2")
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
            "military-hybrid", 25.0,
            "motorcycle", 35.0,
            "bicycle", 12.0,
            "foot", 5.0);
    private static final String DEFAULT_VEHICLE = "offroad-car";
    /**
     * 公路与越野混合路由默认使用的路网 profile。
     */
    private static final String DEFAULT_HYBRID_PROFILE = "driving-military";
    private static final double DEFAULT_OFFROAD_PENALTY = 1.5;
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

    @PostMapping(value = "/offroad", produces = "application/json;charset=UTF-8")
    @Operation(description = "根据起点和终点计算越野路径，返回路径几何、统计信息与坡度超限的障碍区域", summary = "Off Road Service")
    public ResponseEntity<Object> offroad(@RequestBody OffRoadRequest request) {
        return computeOffRoad(request, false);
    }

    @PostMapping(value = "/hybrid", produces = "application/json;charset=UTF-8")
    @Operation(description = "军车公路与越野混合路由：有路走公路按路网速度，无路时按地形坡度越野，返回分段明细",
            summary = "Hybrid Road/Off Road Service")
    public ResponseEntity<Object> hybrid(@RequestBody OffRoadRequest request) {
        return computeOffRoad(request, true);
    }

    private ResponseEntity<Object> computeOffRoad(OffRoadRequest request, boolean forceHybrid) {
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

        // 是否混合由接口决定：/v2/offroad 恒为纯越野，/v2/hybrid 恒为公路+越野
        boolean hybrid = forceHybrid;
        String hybridProfile = request.profile() == null || request.profile().isBlank()
                ? DEFAULT_HYBRID_PROFILE : request.profile().trim();
        double offroadSpeedKmh = request.offroadSpeedKmh() == null ? speedKmh : request.offroadSpeedKmh();
        if (offroadSpeedKmh <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "offroad_speed_kmh 需为正数"));
        }
        double offroadPenalty = request.offroadPenalty() == null ? DEFAULT_OFFROAD_PENALTY : request.offroadPenalty();
        if (offroadPenalty < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "offroad_penalty 不能小于 1"));
        }
        RoadAccessProvider roadAccessProvider = null;
        if (hybrid) {
            try {
                roadAccessProvider = createRoadAccessProvider(hybridProfile, gridInterval);
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
            }
        }
        ghRequest.putHint("hybrid", hybrid);
        ghRequest.putHint("offroad_speed_kmh", offroadSpeedKmh);
        ghRequest.putHint("offroad_penalty", offroadPenalty);

        ElevationProvider elevationProvider = resolveElevationProvider();
        OffRoadResponse offRoadResponse;
        try {
            synchronized (offroadLock) {
                offRoadResponse = new OffRoader(elevationProvider, roadAccessProvider).offroad(ghRequest);
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
        result.put("hybrid", hybrid);
        if (hybrid) {
            result.put("profile", hybridProfile);
            result.put("offroad_speed_kmh", offroadSpeedKmh);
            result.put("offroad_penalty", offroadPenalty);
            // 分段明细：按公路/越野连续段给出里程、耗时与起止坐标
            List<Map<String, Object>> segments = new ArrayList<>();
            int segmentIndex = 0;
            for (OffRoadResponse.Segment segment : offRoadResponse.getSegments()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", ++segmentIndex);
                item.put("type", segment.isOnRoad() ? "road" : "offroad");
                item.put("distance", round1(segment.getDistance()));
                item.put("duration", segment.getDuration());
                item.put("start", List.of(round6(segment.getStartLon()), round6(segment.getStartLat())));
                item.put("end", List.of(round6(segment.getEndLon()), round6(segment.getEndLat())));
                // 该段经过的网格中心点，前端按段着色绘制
                List<List<Double>> segmentCoordinates = new ArrayList<>();
                for (com.graphhopper.util.shapes.GHPoint point : segment.getPoints()) {
                    segmentCoordinates.add(List.of(round6(point.lon), round6(point.lat)));
                }
                item.put("coordinates", segmentCoordinates);
                segments.add(item);
            }
            result.put("segments", segments);
        }
        result.put("point_count", coordinates.size());
        result.put("summary", buildSummary(distance, elevation, offRoadResponse, speedKmh, hybrid));
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
     * 混合路由的耗时取搜索过程中累计的实际用时，并补充公路/越野里程与公路占比。
     */
    private Map<String, Object> buildSummary(double distance, Map<String, Object> elevation,
                                             OffRoadResponse response, double speedKmh, boolean hybrid) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("distance", round1(distance));
        if (hybrid) {
            summary.put("duration", response.getDurationSeconds() > 0
                    ? response.getDurationSeconds()
                    : Math.round(distance / (speedKmh / 3.6)));
            double roadDistance = response.getRoadDistance();
            double offRoadDistance = response.getOffRoadDistance();
            summary.put("road_distance", round1(roadDistance));
            summary.put("offroad_distance", round1(offRoadDistance));
            double total = roadDistance + offRoadDistance;
            summary.put("road_ratio", total > 0 ? round1(roadDistance / total * 100) : 0.0);
        } else {
            // 按所选交通工具的平均速度估算耗时（秒）
            summary.put("duration", Math.round(distance / (speedKmh / 3.6)));
        }
        summary.put("max_slope", elevation == null ? null : elevation.get("max_slope"));
        summary.put("avg_slope", elevation == null ? null : elevation.get("avg_slope"));
        summary.put("ascend", elevation == null ? null : elevation.get("ascend"));
        summary.put("descend", elevation == null ? null : elevation.get("descend"));
        summary.put("barrier_count", response.getBarrierCount());
        return summary;
    }

    /**
     * 按 profile 的路网构建公路信息提供者；profile 未加载或路网缺少对应编码值时抛出参数异常。
     */
    private RoadAccessProvider createRoadAccessProvider(String profileName, double gridInterval) {
        RoutingProfile profile = RoutingProfileManager.getInstance().getRoutingProfile(profileName);
        if (profile == null) {
            throw new IllegalArgumentException("未加载 profile：" + profileName + "，请确认配置中已启用对应交通工具");
        }
        // EncoderNameEnum.getEncoderName() 返回的是 API 名称（如 driving-military），
        // 需要先转成 GraphHopper 侧的 encoder 名称（如 military）再映射到车辆类型（car）
        EncoderNameEnum encoderNameEnum = profile.getProfileProperties().getEncoderName();
        String encoderName = EncoderNameEnum.getEncoderName(encoderNameEnum.getValue());
        String vehicle = FlagEncoderNames.getGHVehicleName(encoderName);
        // 吸附容差取网格边长的 0.75 倍（略大于半对角线），保证斜穿网格的公路也能被标记
        return new GraphRoadAccessProvider(profile.getGraphhopper(), vehicle, gridInterval * 0.75);
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
                                 @JsonProperty("vehicle") String vehicle,
                                 @JsonProperty("hybrid") Boolean hybrid,
                                 @JsonProperty("profile") String profile,
                                 @JsonProperty("offroad_speed_kmh") Double offroadSpeedKmh,
                                 @JsonProperty("offroad_penalty") Double offroadPenalty) {
    }
}
