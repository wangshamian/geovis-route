package org.heigit.geovisroute.routing.graphhopper.extensions;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.parsers.helpers.OSMValueExtractor;
import com.graphhopper.search.KVStorage.KValue;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.RAMDirectory;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import org.heigit.geovisroute.config.profile.CustomRoadNetworkProperties;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.HeavyVehicleAttributesGraphStorage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 自定义路网（GeoJSON）导入器。
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>{@code add}：把 GeoJSON 作为新道路注入路网图，与已有路网在最近的节点上连接。</li>
 *   <li>{@code overlay}：按几何匹配已有 OSM 边，只覆盖限高、限宽、限重等属性，不新增边。</li>
 * </ul>
 * 必须在 GraphHopper 的 importOSM() 之后、postImportOSM()/cleanUp()/postProcessing() 之前调用，
 * 这样新增的边或属性才会参与位置索引、子网络过滤和 CH/Core/等时圈预处理。
 */
public class CustomRoadNetworkImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomRoadNetworkImporter.class);
    private static final double COORD_KEY_SCALE = 1_000_000d;
    private static final double METERS_PER_DEGREE = 111_320d;
    private static final double MIN_PARALLEL_COSINE = 0.7d;

    private static final Map<String, Integer> RESTRICTION_INDEX = Map.of(
            "maxheight", VehicleDimensionRestrictions.MAX_HEIGHT,
            "maxwidth", VehicleDimensionRestrictions.MAX_WIDTH,
            "maxweight", VehicleDimensionRestrictions.MAX_WEIGHT,
            "maxlength", VehicleDimensionRestrictions.MAX_LENGTH,
            "maxaxleload", VehicleDimensionRestrictions.MAX_AXLE_LOAD);
    private static final Map<String, String> ENCODED_VALUE_KEYS = Map.of(
            "maxheight", "max_height",
            "maxwidth", "max_width",
            "maxweight", "max_weight",
            "maxlength", "max_length",
            "maxaxleload", "max_axle_load");
    private static final Set<String> WEIGHT_TAGS = Set.of("maxweight", "maxaxleload");

    private final BaseGraph graph;
    private final EncodingManager encodingManager;
    private final OSMParsers osmParsers;
    private long nextWayId = -1;

    public CustomRoadNetworkImporter(BaseGraph graph, EncodingManager encodingManager, OSMParsers osmParsers) {
        this.graph = graph;
        this.encodingManager = encodingManager;
        this.osmParsers = osmParsers;
    }

    public void importRoads(CustomRoadNetworkProperties config) {
        Path file = Path.of(config.getFile());
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Custom road network file not found: " + file.toAbsolutePath());
        }

        List<RoadFeature> features;
        try {
            features = readFeatures(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read custom road network file: " + file.toAbsolutePath(), e);
        }
        if (features.isEmpty()) {
            LOGGER.warn("No line geometry found in custom road network file: {}", file.toAbsolutePath());
            return;
        }

        String mode = config.getMode() == null ? "add" : config.getMode().trim().toLowerCase(Locale.ROOT);
        switch (mode) {
            case "overlay" -> overlayRoads(features, config, file);
            case "add" -> addRoads(features, config, file);
            default -> throw new IllegalArgumentException(
                    "Unknown custom_roads.mode '" + config.getMode() + "', expected 'add' or 'overlay'");
        }
    }

    private void addRoads(List<RoadFeature> features, CustomRoadNetworkProperties config, Path file) {
        double snapRadius = config.getSnapRadius() == null ? 0d : config.getSnapRadius();
        Map<Long, Integer> customNodes = new HashMap<>();
        LocationIndex locationIndex = snapRadius > 0 ? buildLocationIndex(features, snapRadius) : null;

        int createdNodes = 0;
        int createdEdges = 0;
        int skippedWays = 0;
        try {
            for (RoadFeature feature : features) {
                Map<String, String> tags = mergeTags(config.getTags(), feature.tags);
                for (List<double[]> line : feature.lines) {
                    int previousNode = -1;
                    for (double[] point : line) {
                        int nodesBefore = graph.getNodes();
                        int node = resolveNode(point[0], point[1], snapRadius, locationIndex, customNodes);
                        if (graph.getNodes() > nodesBefore) {
                            createdNodes++;
                        }
                        if (previousNode >= 0 && previousNode != node) {
                            if (createEdge(previousNode, node, tags)) {
                                createdEdges++;
                            } else {
                                skippedWays++;
                            }
                        }
                        previousNode = node;
                    }
                }
            }
        } finally {
            close(locationIndex);
        }

        if (createdNodes == 0 && createdEdges == 0) {
            LOGGER.warn("Custom road network did not create any node or edge, check the GeoJSON content and tags: {}",
                    file.toAbsolutePath());
        } else {
            LOGGER.info("Custom road network added from {}: nodes={}, edges={}, skippedSegments={}, snapRadius={}m",
                    file.toAbsolutePath(), createdNodes, createdEdges, skippedWays, snapRadius);
        }
    }

    private void overlayRoads(List<RoadFeature> features, CustomRoadNetworkProperties config, Path file) {
        double tolerance = config.getMatchTolerance() == null ? 10d : config.getMatchTolerance();
        if (tolerance <= 0) {
            throw new IllegalArgumentException("custom_roads.match_tolerance must be greater than 0");
        }

        List<String> overlayTags = config.getOverlayTags() == null || config.getOverlayTags().isEmpty()
                ? List.of("maxheight", "maxwidth", "maxweight", "maxlength", "maxaxleload")
                : config.getOverlayTags();
        HeavyVehicleAttributesGraphStorage hgvStorage = findUsableHgvStorage();
        Map<String, DecimalEncodedValue> encodedValues = resolveEncodedValues(overlayTags);

        if (hgvStorage == null && encodedValues.isEmpty()) {
            LOGGER.info("Custom road overlay is skipped: neither ext_storages.HeavyVehicle (restrictions: true) "
                    + "nor encoded values {} are available for this profile", ENCODED_VALUE_KEYS.values());
            return;
        }

        LocationIndex locationIndex = buildLocationIndex(features, tolerance);
        int matchedFeatures = 0;
        int matchedEdges = 0;
        int skippedFeatures = 0;
        try {
            for (RoadFeature feature : features) {
                double[] restrictionValues = parseRestrictions(feature.tags, overlayTags);
                if (!hasRestriction(restrictionValues)) {
                    skippedFeatures++;
                    LOGGER.warn("Custom road feature '{}' has none of the overlay tags {}, skipping",
                            feature.tags.get("name"), overlayTags);
                    continue;
                }
                Set<Integer> edgeIds = findMatchingEdges(feature, tolerance, locationIndex);
                if (edgeIds.isEmpty()) {
                    skippedFeatures++;
                    LOGGER.warn("Custom road feature '{}' did not match any existing OSM edge within {}m",
                            feature.tags.get("name"), tolerance);
                    continue;
                }
                matchedFeatures++;
                for (int edgeId : edgeIds) {
                    matchedEdges += applyToEdge(edgeId, restrictionValues, hgvStorage, encodedValues);
                }
            }
        } finally {
            close(locationIndex);
        }

        LOGGER.info("Custom road overlay from {}: matchedFeatures={}, matchedEdges={}, skippedFeatures={}, "
                        + "tolerance={}m, hgvStorage={}, encodedValues={}",
                file.toAbsolutePath(), matchedFeatures, matchedEdges, skippedFeatures, tolerance,
                hgvStorage != null, encodedValues.keySet());
    }

    private HeavyVehicleAttributesGraphStorage findUsableHgvStorage() {
        if (graph.getExtensions() == null) {
            return null;
        }
        HeavyVehicleAttributesGraphStorage storage = null;
        for (GraphExtension extension : graph.getExtensions().getExtensions()) {
            if (extension instanceof HeavyVehicleAttributesGraphStorage hgv) {
                storage = hgv;
                break;
            }
        }
        if (storage == null) {
            return null;
        }
        try {
            storage.getEdgeRestrictionValues(0, new double[VehicleDimensionRestrictions.COUNT]);
            return storage;
        } catch (RuntimeException e) {
            LOGGER.warn("HeavyVehicle storage exists but restrictions are disabled: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, DecimalEncodedValue> resolveEncodedValues(List<String> overlayTags) {
        Map<String, DecimalEncodedValue> result = new LinkedHashMap<>();
        for (String tag : overlayTags) {
            String key = ENCODED_VALUE_KEYS.get(tag);
            if (key == null || !encodingManager.hasEncodedValue(key)) {
                continue;
            }
            result.put(tag, encodingManager.getDecimalEncodedValue(key));
        }
        return result;
    }

    private double[] parseRestrictions(Map<String, String> tags, List<String> overlayTags) {
        double[] values = new double[VehicleDimensionRestrictions.COUNT];
        for (String tag : overlayTags) {
            if (!RESTRICTION_INDEX.containsKey(tag)) {
                LOGGER.warn("Unsupported custom_roads.overlay_tags entry '{}', expected one of {}",
                        tag, RESTRICTION_INDEX.keySet());
                continue;
            }
            String raw = tags.get(tag);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            double value = WEIGHT_TAGS.contains(tag)
                    ? OSMValueExtractor.stringToTons(raw)
                    : OSMValueExtractor.stringToMeter(raw);
            if (Double.isNaN(value) || value <= 0) {
                LOGGER.warn("Cannot parse '{}' value '{}' for custom road feature '{}', skipping this tag",
                        tag, raw, tags.get("name"));
                continue;
            }
            values[RESTRICTION_INDEX.get(tag)] = value;
        }
        return values;
    }

    private boolean hasRestriction(double[] values) {
        for (double value : values) {
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    private Set<Integer> findMatchingEdges(RoadFeature feature, double tolerance, LocationIndex locationIndex) {
        Set<Integer> result = new LinkedHashSet<>();
        for (List<double[]> line : feature.lines) {
            for (int i = 1; i < line.size(); i++) {
                double[] start = line.get(i - 1);
                double[] end = line.get(i);
                double segmentDistance = DistanceCalcEarth.DIST_EARTH.calcDist(
                        start[0], start[1], end[0], end[1]);
                if (segmentDistance < 0.5) {
                    continue;
                }
                int samples = Math.max(2, (int) Math.ceil(segmentDistance / Math.max(5d, tolerance / 2d)));
                for (int sample = 0; sample <= samples; sample++) {
                    double ratio = (double) sample / samples;
                    double lat = start[0] + (end[0] - start[0]) * ratio;
                    double lon = start[1] + (end[1] - start[1]) * ratio;
                    Snap snap = locationIndex.findClosest(lat, lon, EdgeFilter.ALL_EDGES);
                    if (!snap.isValid() || snap.getQueryDistance() > tolerance) {
                        continue;
                    }
                    EdgeIteratorState edge = snap.getClosestEdge();
                    if (edge == null || !isParallel(edge, start, end)) {
                        continue;
                    }
                    result.add(edge.getEdge());
                }
            }
        }
        return result;
    }

    private boolean isParallel(EdgeIteratorState edge, double[] start, double[] end) {
        double cosLat = Math.cos(Math.toRadians((start[0] + end[0]) / 2));
        double segmentX = (end[1] - start[1]) * cosLat;
        double segmentY = end[0] - start[0];

        int base = edge.getBaseNode();
        int adj = edge.getAdjNode();
        double edgeX = (graph.getNodeAccess().getLon(adj) - graph.getNodeAccess().getLon(base)) * cosLat;
        double edgeY = graph.getNodeAccess().getLat(adj) - graph.getNodeAccess().getLat(base);

        double segmentLength = Math.hypot(segmentX, segmentY);
        double edgeLength = Math.hypot(edgeX, edgeY);
        if (segmentLength < 1e-12 || edgeLength < 1e-12) {
            return true;
        }
        double cosine = (segmentX * edgeX + segmentY * edgeY) / (segmentLength * edgeLength);
        return Math.abs(cosine) >= MIN_PARALLEL_COSINE;
    }

    private int applyToEdge(int edgeId, double[] restrictionValues,
                            HeavyVehicleAttributesGraphStorage hgvStorage,
                            Map<String, DecimalEncodedValue> encodedValues) {
        boolean applied = false;

        if (hgvStorage != null) {
            double[] existing = new double[VehicleDimensionRestrictions.COUNT];
            hgvStorage.getEdgeRestrictionValues(edgeId, existing);
            boolean changed = false;
            for (int i = 0; i < restrictionValues.length; i++) {
                if (restrictionValues[i] > 0) {
                    existing[i] = restrictionValues[i];
                    changed = true;
                }
            }
            if (changed) {
                hgvStorage.setEdgeValue(edgeId, hgvStorage.getEdgeVehicleType(edgeId), existing);
                applied = true;
            }
        }

        for (Map.Entry<String, DecimalEncodedValue> entry : encodedValues.entrySet()) {
            int index = RESTRICTION_INDEX.get(entry.getKey());
            if (restrictionValues[index] > 0) {
                entry.getValue().setDecimal(false, edgeId, graph.getEdgeAccess(), restrictionValues[index]);
                applied = true;
            }
        }
        return applied ? 1 : 0;
    }

    private int resolveNode(double lat, double lon, double snapRadius, LocationIndex locationIndex,
                            Map<Long, Integer> customNodes) {
        long key = coordinateKey(lat, lon);
        Integer cached = customNodes.get(key);
        if (cached != null) {
            return cached;
        }

        int snapped = findSnappedNode(lat, lon, snapRadius, locationIndex);
        if (snapped >= 0) {
            return snapped;
        }

        int node = graph.getNodes();
        graph.getNodeAccess().setNode(node, lat, lon);
        customNodes.put(key, node);
        return node;
    }

    private int findSnappedNode(double lat, double lon, double snapRadius, LocationIndex locationIndex) {
        if (snapRadius <= 0 || locationIndex == null) {
            return -1;
        }
        Snap snap = locationIndex.findClosest(lat, lon, EdgeFilter.ALL_EDGES);
        if (!snap.isValid()) {
            return -1;
        }

        int candidate;
        if (snap.getSnappedPosition() == Snap.Position.TOWER) {
            candidate = snap.getClosestNode();
        } else {
            EdgeIteratorState edge = snap.getClosestEdge();
            if (edge == null) {
                return -1;
            }
            candidate = nearerNode(edge, lat, lon);
        }
        if (candidate < 0) {
            return -1;
        }

        double distance = DistanceCalcEarth.DIST_EARTH.calcDist(lat, lon,
                graph.getNodeAccess().getLat(candidate), graph.getNodeAccess().getLon(candidate));
        return distance <= snapRadius ? candidate : -1;
    }

    private int nearerNode(EdgeIteratorState edge, double lat, double lon) {
        int base = edge.getBaseNode();
        int adj = edge.getAdjNode();
        double baseDistance = DistanceCalcEarth.DIST_EARTH.calcDist(lat, lon,
                graph.getNodeAccess().getLat(base), graph.getNodeAccess().getLon(base));
        double adjDistance = DistanceCalcEarth.DIST_EARTH.calcDist(lat, lon,
                graph.getNodeAccess().getLat(adj), graph.getNodeAccess().getLon(adj));
        return baseDistance <= adjDistance ? base : adj;
    }

    private boolean createEdge(int from, int to, Map<String, String> tags) {
        double fromLat = graph.getNodeAccess().getLat(from);
        double fromLon = graph.getNodeAccess().getLon(from);
        double toLat = graph.getNodeAccess().getLat(to);
        double toLon = graph.getNodeAccess().getLon(to);
        double distance = Math.max(0.001, DistanceCalcEarth.DIST_EARTH.calcDist(fromLat, fromLon, toLat, toLon));

        PointList pointList = new PointList(2, false);
        pointList.add(fromLat, fromLon);
        pointList.add(toLat, toLon);

        ReaderWay way = new ReaderWay(nextWayId--);
        tags.forEach(way::setTag);
        way.setTag("node_tags", Collections.emptyList());
        way.setTag("edge_distance", distance);
        way.setTag("point_list", pointList);

        if (!osmParsers.acceptWay(way)) {
            LOGGER.debug("Custom road segment is not accepted by any encoder, tags={}", tags);
            return false;
        }

        EdgeIteratorState edge = graph.edge(from, to).setDistance(distance);
        osmParsers.handleWayTags(edge.getEdge(), graph.getEdgeAccess(), way, osmParsers.createRelationFlags());
        String name = tags.get("name");
        if (name != null && !name.isBlank()) {
            edge.setKeyValues(Map.of(Parameters.Details.STREET_NAME, new KValue(name)));
        }
        return true;
    }

    private Map<String, String> mergeTags(Map<String, String> defaultTags, Map<String, String> featureTags) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (defaultTags != null) {
            tags.putAll(defaultTags);
        }
        tags.putAll(featureTags);
        boolean routable = tags.keySet().stream().anyMatch(key ->
                "highway".equals(key) || "route".equals(key) || "railway".equals(key)
                        || "man_made".equals(key) || "waterway".equals(key));
        if (!routable) {
            tags.put("highway", "track");
        }
        return tags;
    }

    private LocationIndex buildLocationIndex(List<RoadFeature> features, double snapRadius) {
        double[] bounds = bounds(features, snapRadius);
        EdgeFilter bboxFilter = edge -> {
            int base = edge.getBaseNode();
            int adj = edge.getAdjNode();
            return inBounds(bounds,
                    graph.getNodeAccess().getLat(base), graph.getNodeAccess().getLon(base))
                    || inBounds(bounds,
                    graph.getNodeAccess().getLat(adj), graph.getNodeAccess().getLon(adj));
        };
        LocationIndexTree index = new LocationIndexTree(graph, new RAMDirectory());
        return index.prepareIndex(bboxFilter);
    }

    private boolean inBounds(double[] bounds, double lat, double lon) {
        return lat >= bounds[0] && lat <= bounds[1] && lon >= bounds[2] && lon <= bounds[3];
    }

    private double[] bounds(List<RoadFeature> features, double snapRadius) {
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        for (RoadFeature feature : features) {
            for (List<double[]> line : feature.lines) {
                for (double[] point : line) {
                    minLat = Math.min(minLat, point[0]);
                    maxLat = Math.max(maxLat, point[0]);
                    minLon = Math.min(minLon, point[1]);
                    maxLon = Math.max(maxLon, point[1]);
                }
            }
        }
        double latMargin = snapRadius / METERS_PER_DEGREE + 0.001;
        double midLat = (minLat + maxLat) / 2;
        double lonMargin = snapRadius / (METERS_PER_DEGREE * Math.cos(Math.toRadians(midLat))) + 0.001;
        return new double[]{minLat - latMargin, maxLat + latMargin, minLon - lonMargin, maxLon + lonMargin};
    }

    private void close(LocationIndex locationIndex) {
        if (locationIndex != null) {
            locationIndex.close();
        }
    }

    private long coordinateKey(double lat, double lon) {
        long latKey = Math.round(lat * COORD_KEY_SCALE);
        long lonKey = Math.round(lon * COORD_KEY_SCALE);
        return (latKey << 32) | (lonKey & 0xFFFFFFFFL);
    }

    private List<RoadFeature> readFeatures(Path file) throws IOException {
        JSONObject root = new JSONObject(Files.readString(file));
        List<RoadFeature> features = new ArrayList<>();
        String type = root.optString("type");
        if ("FeatureCollection".equals(type)) {
            JSONArray array = root.optJSONArray("features");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    addFeature(array.optJSONObject(i), features);
                }
            }
        } else if ("Feature".equals(type)) {
            addFeature(root, features);
        } else {
            addGeometry(root, Collections.emptyMap(), features);
        }
        return features;
    }

    private void addFeature(JSONObject feature, List<RoadFeature> features) {
        if (feature == null) {
            return;
        }
        JSONObject geometry = feature.optJSONObject("geometry");
        if (geometry == null) {
            return;
        }
        addGeometry(geometry, properties(feature.optJSONObject("properties")), features);
    }

    private void addGeometry(JSONObject geometry, Map<String, String> tags, List<RoadFeature> features) {
        String type = geometry.optString("type");
        if ("LineString".equals(type)) {
            List<double[]> line = readLine(geometry.optJSONArray("coordinates"));
            if (line.size() > 1) {
                RoadFeature feature = new RoadFeature(tags);
                feature.lines.add(line);
                features.add(feature);
            }
        } else if ("MultiLineString".equals(type)) {
            JSONArray lines = geometry.optJSONArray("coordinates");
            RoadFeature feature = new RoadFeature(tags);
            if (lines != null) {
                for (int i = 0; i < lines.length(); i++) {
                    List<double[]> line = readLine(lines.optJSONArray(i));
                    if (line.size() > 1) {
                        feature.lines.add(line);
                    }
                }
            }
            if (!feature.lines.isEmpty()) {
                features.add(feature);
            }
        } else if ("GeometryCollection".equals(type)) {
            JSONArray geometries = geometry.optJSONArray("geometries");
            if (geometries != null) {
                for (int i = 0; i < geometries.length(); i++) {
                    addGeometry(geometries.optJSONObject(i), tags, features);
                }
            }
        } else {
            LOGGER.warn("Unsupported GeoJSON geometry type: {}", type);
        }
    }

    private List<double[]> readLine(JSONArray coordinates) {
        List<double[]> line = new ArrayList<>();
        if (coordinates == null) {
            return line;
        }
        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray position = coordinates.optJSONArray(i);
            if (position == null || position.length() < 2) {
                continue;
            }
            line.add(new double[]{position.getDouble(1), position.getDouble(0)});
        }
        return line;
    }

    private Map<String, String> properties(JSONObject properties) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (properties == null) {
            return tags;
        }
        for (String key : properties.keySet()) {
            Object value = properties.opt(key);
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                tags.put(key, String.valueOf(value));
            }
        }
        return tags;
    }

    private static final class RoadFeature {
        private final Map<String, String> tags;
        private final List<List<double[]>> lines = new ArrayList<>();

        private RoadFeature(Map<String, String> tags) {
            this.tags = tags;
        }
    }
}
