/*
 * This file is part of Openrouteservice.
 *
 * Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, see <https://www.gnu.org/licenses/>.
 */

package org.heigit.geovisroute.api.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OpenStreetMap 瓦片本地缓存接口。
 * 首次请求某瓦片时从上游拉取并写入本地磁盘，之后直接读取本地文件，实现底图离线持久化。
 */
@RestController
@Tag(name = "Tile cache service", description = "OpenStreetMap 瓦片本地缓存")
@RequestMapping("/v2/tiles/osm")
public class TileCacheAPI {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Path osmCacheDir;
    private final String upstreamUrl;
    private final int maxZoom;
    private final String userAgent;

    public TileCacheAPI(@Value("${ors.tiles.cache_path:./tile-cache}") String cachePath,
                        @Value("${ors.tiles.osm_upstream:https://tile.openstreetmap.org/{z}/{x}/{y}.png}") String upstreamUrl,
                        @Value("${ors.tiles.osm_max_zoom:19}") int maxZoom,
                        @Value("${ors.tiles.user_agent:geovis-route-local-tile-cache/1.0}") String userAgent) {
        this.osmCacheDir = Paths.get(cachePath, "osm").toAbsolutePath().normalize();
        this.upstreamUrl = upstreamUrl;
        this.maxZoom = maxZoom;
        this.userAgent = userAgent;
    }

    @GetMapping("/{z}/{x}/{y}.png")
    public ResponseEntity<byte[]> getTile(@PathVariable("z") int z,
                                          @PathVariable("x") int x,
                                          @PathVariable("y") int y) throws IOException, InterruptedException {
        int tileCount = z >= 0 && z <= 30 ? 1 << z : 0;
        if (z < 0 || z > maxZoom || x < 0 || y < 0 || x >= tileCount || y >= tileCount)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Path tileFile = osmCacheDir.resolve(String.valueOf(z)).resolve(String.valueOf(x)).resolve(y + ".png");

        byte[] data = readCached(tileFile);
        if (data == null) {
            data = download(z, x, y);
            if (data == null)
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            writeCache(tileFile, data);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(data);
    }

    private byte[] readCached(Path tileFile) {
        try {
            return Files.isRegularFile(tileFile) ? Files.readAllBytes(tileFile) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] download(int z, int x, int y) {
        String url = upstreamUrl.replace("{z}", String.valueOf(z))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y));
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() == HttpStatus.OK.value() ? response.body() : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void writeCache(Path tileFile, byte[] data) {
        try {
            Files.createDirectories(tileFile.getParent());
            Path tmpFile = Files.createTempFile(tileFile.getParent(), "tile-", ".tmp");
            Files.write(tmpFile, data);
            try {
                Files.move(tmpFile, tileFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                // 某些文件系统不支持原子覆盖，退化为普通移动
                Files.move(tmpFile, tileFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // 缓存写入失败不影响本次返回
        }
    }
}
