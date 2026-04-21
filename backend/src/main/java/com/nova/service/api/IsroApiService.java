package com.nova.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.model.SpaceBriefCache.Launch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IsroApiService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${isro.base-url}")
    private String isroBaseUrl;

    @Value("${the-space-devs.base-url}")
    private String spaceDevsBaseUrl;

    @Value("${the-space-devs.api-key:}")
    private String spaceDevsApiKey;

    // ─── ISRO Community API ───────────────────────────────────────────────────

    /**
     * Fetches ISRO spacecrafts / satellites from the community API.
     * isro.vercel.app is a free, no-key community-maintained data source.
     */
    @Cacheable(value = "missions", key = "'isro-spacecraft'")
    public List<IsroSpacecraft> fetchIsroSpacecrafts() {
        String url = isroBaseUrl + "/spacecrafts";
        List<IsroSpacecraft> result = new ArrayList<>();
        try {
            JsonNode root = get(url);
            if (root.isArray()) {
                root.forEach(item -> result.add(new IsroSpacecraft(
                        item.path("name").asText(),
                        item.path("alias").asText(""),
                        item.path("launchDate").asText(""),
                        item.path("launchVehicle").asText(""),
                        item.path("orbit").asText(""),
                        item.path("status").asText("Active")
                )));
            }
        } catch (Exception e) {
            log.error("ISRO spacecraft fetch failed: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Fetches upcoming ISRO launches from The Space Devs API
     * (which aggregates from all agencies including ISRO).
     * Cached for 5 minutes.
     */
    @Cacheable(value = "launches", key = "'isro'")
    public List<Launch> fetchIsroUpcomingLaunches() {
        // Filter by ISRO's agency id = 31 on The Space Devs
        String url = spaceDevsBaseUrl
                + "/launch/upcoming/?agency__abbrev=ISRO&limit=5&ordering=window_start";
        return fetchSpaceDevsLaunches(url, "ISRO", "ISRO");
    }

    /**
     * Fetches upcoming NASA launches from The Space Devs API.
     */
    @Cacheable(value = "launches", key = "'nasa'")
    public List<Launch> fetchNasaUpcomingLaunches() {
        String url = spaceDevsBaseUrl
                + "/launch/upcoming/?agency__abbrev=NASA&limit=5&ordering=window_start";
        return fetchSpaceDevsLaunches(url, "NASA", "NASA");
    }

    // ─── Shared Space Devs parser ─────────────────────────────────────────────

    private List<Launch> fetchSpaceDevsLaunches(String url, String agency, String agencyCode) {
        List<Launch> launches = new ArrayList<>();
        try {
            Request.Builder reqBuilder = new Request.Builder().url(url);
            if (!spaceDevsApiKey.isEmpty()) {
                reqBuilder.addHeader("Authorization", "Token " + spaceDevsApiKey);
            }
            try (Response res = httpClient.newCall(reqBuilder.build()).execute()) {
                if (!res.isSuccessful()) {
                    log.warn("Space Devs API returned {}", res.code());
                    return launches;
                }
                JsonNode root = objectMapper.readTree(res.body().string());
                JsonNode results = root.path("results");
                if (results.isArray()) {
                    results.forEach(item -> {
                        Instant windowStart = null;
                        String ws = item.path("window_start").asText("");
                        if (!ws.isEmpty()) {
                            try { windowStart = Instant.parse(ws); }
                            catch (Exception ignored) {}
                        }
                        String statusName = item.path("status").path("name").asText("TBD");
                        String launchSite = item.path("pad").path("name").asText("TBD");
                        String country    = item.path("pad").path("location")
                                               .path("country_code").asText("");
                        launches.add(Launch.builder()
                                .id(item.path("id").asText())
                                .name(item.path("name").asText("Unknown"))
                                .agency(agency)
                                .agencyCode(agencyCode)
                                .rocket(item.path("rocket").path("configuration")
                                            .path("name").asText("Unknown"))
                                .launchSite(launchSite)
                                .launchSiteCountry(country)
                                .windowStart(windowStart)
                                .status(mapStatus(statusName))
                                .missionDesc(item.path("mission").path("description").asText("TBD"))
                                .imageUrl(item.path("image").asText(""))
                                .build());
                    });
                }
            }
        } catch (Exception e) {
            log.error("{} launch fetch failed: {}", agency, e.getMessage());
        }
        return launches;
    }

    private String mapStatus(String name) {
        if (name == null) return "TBD";
        return switch (name.toLowerCase()) {
            case "go for launch" -> "Go";
            case "success"       -> "Success";
            case "failure"       -> "Failure";
            case "hold"          -> "Hold";
            default              -> "TBD";
        };
    }

    private JsonNode get(String url) throws Exception {
        Request req = new Request.Builder().url(url).get().build();
        try (Response res = httpClient.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new RuntimeException("HTTP " + res.code());
            return objectMapper.readTree(res.body().string());
        }
    }

    // ─── ISRO Spacecraft DTO ──────────────────────────────────────────────────
    public record IsroSpacecraft(
            String name, String alias, String launchDate,
            String launchVehicle, String orbit, String status) {}
}
