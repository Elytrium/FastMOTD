package net.elytrium.fastmotd.utils;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeoIpResolver implements AutoCloseable {

    private final DatabaseReader reader;

    public GeoIpResolver(Path databasePath) throws IOException {
        if (!Files.exists(databasePath)) {
            throw new IOException("GeoIP database not found: " + databasePath);
        }
        if (Files.size(databasePath) < 1024) {
            throw new IOException("GeoIP database looks too small (possibly corrupt): " + databasePath);
        }

        // CHMCache заметно ускоряет повторные запросы с тех же IP
        this.reader = new DatabaseReader.Builder(databasePath.toFile())
                .withCache(new CHMCache(4096))
                .build();
    }

    /**
     * @return ISO 3166-1 alpha-2 country code ("RU", "DE"), or null if unknown.
     */
    public String resolveCountry(InetAddress address) {
        if (address == null) {
            return null;
        }
        try {
            CountryResponse response = this.reader.country(address);
            String iso = response.getCountry().getIsoCode();
            return (iso == null || iso.isEmpty()) ? null : iso;
        } catch (IOException | GeoIp2Exception e) {
            // GeoIp2Exception = private/reserved IP or not found in DB -> normal, just return null
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}