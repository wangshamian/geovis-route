package org.heigit.geovisroute.coordinates_generator.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouteRepositoryTest {

    private RouteRepository repository;
    private final Set<String> profiles = Set.of("driving-car", "cycling-regular");

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        repository = new RouteRepository(profiles);
    }

    @Test
    void testAddRoute() {
        double[] start = {8.681495, 49.41461};
        double[] end = {8.686507, 49.41943};
        double distance = 1500.5;
        String profile = "driving-car";

        Route route = new Route(start, end, distance, profile);
        boolean added = repository.addRoute(route);

        assertTrue(added);
        assertEquals(1, repository.getRouteCount(profile));
        assertEquals(0, repository.getRouteCount("cycling-regular"));
    }

    @Test
    void testAddDuplicateRoute() {
        double[] start = {8.681495, 49.41461};
        double[] end = {8.686507, 49.41943};
        double distance = 1500.5;
        String profile = "driving-car";

        Route route1 = new Route(start, end, distance, profile);
        Route route2 = new Route(start, end, distance, profile);

        assertTrue(repository.addRoute(route1));
        assertFalse(repository.addRoute(route2));
        assertEquals(1, repository.getRouteCount(profile));
    }

    @Test
    void testAddRouteIfUnique() {
        double[] start = {8.681495, 49.41461};
        double[] end = {8.686507, 49.41943};
        double distance = 1500.5;
        String profile = "driving-car";

        Route route = new Route(start, end, distance, profile);
        boolean added = repository.addRouteIfUnique(route);

        assertTrue(added);
        assertEquals(1, repository.getRouteCount(profile));
    }

    @Test
    void testClear() {
        // 添加一些路线
        for (String profile : profiles) {
            for (int i = 0; i < 5; i++) {
                double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
                double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
                repository.addRoute(new Route(start, end, 1500.5 + i * 100, profile));
            }
        }

        assertEquals(5, repository.getRouteCount("driving-car"));
        assertEquals(5, repository.getRouteCount("cycling-regular"));
        assertEquals(10, repository.getTotalRouteCount());

        repository.clear();

        assertEquals(0, repository.getRouteCount("driving-car"));
        assertEquals(0, repository.getRouteCount("cycling-regular"));
        assertEquals(0, repository.getTotalRouteCount());
    }

    @Test
    void testGetTotalRouteCount() {
        // 添加一些路线
        for (String profile : profiles) {
            for (int i = 0; i < 3; i++) {
                double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
                double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
                repository.addRoute(new Route(start, end, 1500.5 + i * 100, profile));
            }
        }

        assertEquals(6, repository.getTotalRouteCount());
    }

    @Test
    void testIsProfileComplete() {
        // 仅添加 driving-car 的路线
        for (int i = 0; i < 5; i++) {
            double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
            double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
            repository.addRoute(new Route(start, end, 1500.5 + i * 100, "driving-car"));
        }

        assertTrue(repository.isProfileComplete("driving-car", 5));
        assertFalse(repository.isProfileComplete("driving-car", 6));
        assertFalse(repository.isProfileComplete("cycling-regular", 1));
    }

    @Test
    void testAreAllProfilesComplete() {
        // 为所有配置方案添加路线
        for (String profile : profiles) {
            for (int i = 0; i < 5; i++) {
                double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
                double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
                repository.addRoute(new Route(start, end, 1500.5 + i * 100, profile));
            }
        }

        assertTrue(repository.areAllProfilesComplete(5));
        assertFalse(repository.areAllProfilesComplete(6));
    }

    @Test
    void testGetAllRoutes() {
        // 为所有配置方案添加路线
        for (String profile : profiles) {
            for (int i = 0; i < 5; i++) {
                double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
                double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
                repository.addRoute(new Route(start, end, 1500.5 + i * 100, profile));
            }
        }

        List<Route> allRoutes = repository.getAllRoutes(10);
        assertEquals(10, allRoutes.size());

        // 测试限制数量
        List<Route> limitedRoutes = repository.getAllRoutes(3);
        assertEquals(6, limitedRoutes.size()); // 每个配置方案 3 条
    }

    @Test
    void testWriteToCSV(@TempDir Path tempDir) throws IOException {
        // 为所有配置方案添加路线
        for (String profile : profiles) {
            for (int i = 0; i < 2; i++) {
                double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
                double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
                repository.addRoute(new Route(start, end, 1500.5 + i * 100, profile));
            }
        }

        String filename = tempDir.resolve("routes.csv").toString();
        repository.writeToCSV(filename);

        List<String> lines = Files.readAllLines(Path.of(filename));
        assertEquals(5, lines.size()); // 表头 + 4 条路线
        assertTrue(lines.get(0).startsWith("start_longitude,start_latitude"));
        for (int i = 1; i < lines.size(); i++) {
            assertTrue(lines.get(i).contains("driving-car") || lines.get(i).contains("cycling-regular"));
        }

        // 测试限制数量
        String limitedFilename = tempDir.resolve("routes_limited.csv").toString();
        repository.writeToCSV(limitedFilename, 1);

        lines = Files.readAllLines(Path.of(limitedFilename));
        assertEquals(3, lines.size()); // 表头 + 2 条路线（每个配置方案 1 条）
    }

    @Test
    void testGetProgressMessage() {
        // 为每个配置方案添加不同数量的路线
        for (int i = 0; i < 3; i++) {
            double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
            double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
            repository.addRoute(new Route(start, end, 1500.5 + i * 100, "driving-car"));
        }
        
        for (int i = 0; i < 2; i++) {
            double[] start = {8.681495 + i * 0.001, 49.41461 + i * 0.001};
            double[] end = {8.686507 + i * 0.001, 49.41943 + i * 0.001};
            repository.addRoute(new Route(start, end, 1500.5 + i * 100, "cycling-regular"));
        }

        String message = repository.getProgressMessage();
        assertTrue(message.contains("driving-car: 3"));
        assertTrue(message.contains("cycling-regular: 2"));
    }

    @Test
    void testRepositoryWithUnknownProfile() {
        Set<String> definedProfiles = new HashSet<>(profiles);
        RouteRepository testRepo = new RouteRepository(definedProfiles);
        
        // 尝试为未定义的配置方案添加路线
        double[] start = {8.681495, 49.41461};
        double[] end = {8.686507, 49.41943};
        Route route = new Route(start, end, 1500.5, "undefined-profile");
        
        // 这不应抛出异常，但添加应失败
        boolean added = testRepo.addRoute(route);
        assertFalse(added);
    }
}
