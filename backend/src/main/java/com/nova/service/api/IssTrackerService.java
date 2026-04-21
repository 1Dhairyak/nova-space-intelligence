package com.nova.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.model.SpaceBriefCache.IssPosition;
import com.nova.model.SpaceBriefCache.SpaceWeather;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssTrackerService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${open-notify.base-url}")
    private String openNotifyUrl;

    // ISS cruises at ~7.66 km/s at ~408 km altitude — constant for display
    private static final double ISS_SPEED_KMS    = 7.66;
    private static final double ISS_ALTITUDE_KM  = 408.0;

    /**
     * Fetches live ISS position from Open Notify API.
     * No API key required. Called every 5 seconds by the scheduler.
     * Result is NOT cached via @Cacheable — the scheduler stores it
     * directly in Redis with a 10-second TTL via RedisTemplate.
     */
    public IssPosition fetchIssPosition() {
        String url = openNotifyUrl + "/iss-now.json";
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response res = httpClient.newCall(req).execute()) {
                if (!res.isSuccessful()) throw new RuntimeException("HTTP " + res.code());
                JsonNode root = objectMapper.readTree(res.body().string());
                JsonNode pos  = root.path("iss_position");
                return IssPosition.builder()
                        .latitude(pos.path("latitude").asDouble())
                        .longitude(pos.path("longitude").asDouble())
                        .altitudeKm(ISS_ALTITUDE_KM)
                        .speedKms(ISS_SPEED_KMS)
                        .timestamp(Instant.now())
                        .build();
            }
        } catch (Exception e) {
            log.warn("ISS fetch failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetches current geomagnetic Kp index from NOAA.
     * Used to determine aurora / solar storm severity.
     */
    public SpaceWeather buildSpaceWeather(List<SpaceWeather.SolarFlare> flares) {
        double kp = fetchKpIndex();
        return SpaceWeather.builder()
                .kpIndex(kp)
                .stormLevel(kpToStormLevel(kp))
                .flares(flares)
                .fetchedAt(Instant.now())
                .build();
    }

    private double fetchKpIndex() {
        // NOAA real-time Kp index JSON feed
        String url = "https://services.swpc.noaa.gov/json/planetary_k_index_1m.json";
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response res = httpClient.newCall(req).execute()) {
                if (!res.isSuccessful()) return 0.0;
                JsonNode arr = objectMapper.readTree(res.body().string());
                if (arr.isArray() && arr.size() > 0) {
                    // Last element is the most recent reading
                    JsonNode last = arr.get(arr.size() - 1);
                    return last.path("kp_index").asDouble(0.0);
                }
            }
        } catch (Exception e) {
            log.warn("Kp index fetch failed: {}", e.getMessage());
        }
        return 0.0;
    }

    private String kpToStormLevel(double kp) {
        if (kp >= 9.0) return "G5";
        if (kp >= 8.0) return "G4";
        if (kp >= 7.0) return "G3";
        if (kp >= 6.0) return "G2";
        if (kp >= 5.0) return "G1";
        return "None";
    }
}
