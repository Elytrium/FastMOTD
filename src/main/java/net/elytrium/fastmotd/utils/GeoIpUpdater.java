/*
 * Copyright (C) 2022 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.fastmotd.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;

public class GeoIpUpdater {

    // Fixed URLs pointing to the "latest" release assets
    private static final String MAXMIND_URL =
            "https://github.com/jcjc-dev/mmdb-latest/releases/download/maxmind-latest/GeoLite2-Country.mmdb";
    private static final String DBIP_URL =
            "https://github.com/jcjc-dev/mmdb-latest/releases/download/dbip-latest/dbip-country-lite.mmdb";

    private static final String MAXMIND_FILENAME = "GeoLite2-Country.mmdb";
    private static final String DBIP_FILENAME = "dbip-country-lite.mmdb";

    private final FastMOTD plugin;
    private final Path dataDirectory;

    public GeoIpUpdater(FastMOTD plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.dataDirectory = dataDirectory;
    }

    /**
     * Ensures the GeoIP database exists locally and is fresh enough.
     * Thread-safe: guarded by 'this' to avoid duplicate concurrent downloads.
     *
     * @return the path to the ready-to-use .mmdb, or null on failure.
     */
    public synchronized Path ensureDatabase() {
        if (!Settings.IMP.MAIN.GEOIP_AUTO_UPDATE) {
            String configured = Settings.IMP.MAIN.GEOIP_DATABASE;
            if (configured == null || configured.isEmpty()) {
                return null;
            }
            return this.dataDirectory.resolve(configured);
        }

        Settings.GeoIpSource source = Settings.IMP.MAIN.GEOIP_AUTO_UPDATE_SOURCE;
        String url = (source == Settings.GeoIpSource.MAXMIND) ? MAXMIND_URL : DBIP_URL;
        String filename = (source == Settings.GeoIpSource.MAXMIND) ? MAXMIND_FILENAME : DBIP_FILENAME;
        Path target = this.dataDirectory.resolve(filename);

        if (Files.exists(target)) {
            try {
                Instant lastModified = Files.getLastModifiedTime(target).toInstant();
                long ageDays = Duration.between(lastModified, Instant.now()).toDays();
                if (ageDays < Settings.IMP.MAIN.GEOIP_UPDATE_INTERVAL_DAYS) {
                    this.plugin.getLogger().info(
                            "GeoIP database is {} days old, skipping download", ageDays);
                    return target;
                }
            } catch (IOException e) {
                this.plugin.getLogger().warn("Failed to read GeoIP file age, will re-download", e);
            }
        }

        this.plugin.getLogger().info("Downloading GeoIP database from {}", url);
        try {
            if (this.download(url, target)) {
                this.plugin.getLogger().info("GeoIP database downloaded to {}", target);
                return target;
            }
        } catch (InterruptedException e) {
            this.plugin.getLogger().warn("GeoIP download interrupted");
            Thread.currentThread().interrupt();
        }
        return Files.exists(target) ? target : null;
    }

    private boolean download(String url, Path target) throws InterruptedException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            String version = Settings.IMP.VERSION;
            if (version == null || version.isEmpty()) {
                version = "dev";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .header("User-Agent", "FastMOTD/" + version)
                    .GET()
                    .build();

            Path temp = target.resolveSibling(target.getFileName() + ".tmp");

            HttpResponse<InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                this.plugin.getLogger().warn("GeoIP download failed: HTTP {}", response.statusCode());
                return false;
            }

            try (InputStream in = response.body()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            if (Files.size(temp) < 1024) {
                this.plugin.getLogger().warn("Downloaded GeoIP database looks too small, aborting");
                Files.deleteIfExists(temp);
                return false;
            }

            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            this.plugin.getLogger().warn("Failed to download GeoIP database", e);
            return false;
        } catch (InterruptedException e) {
            throw e;
        }
    }
}