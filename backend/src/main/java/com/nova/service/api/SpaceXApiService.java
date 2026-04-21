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
public class SpaceXApiService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${spacex.base-url}")
    private String baseUrl;

    /**
     * Fetches upcoming SpaceX launches from the public SpaceX v4 API.
     * No API key required. Returns next 10 launches sorted by date.
     * Cached for 5 minutes.
     */
    @Cacheable(value = "launches", key = "'spacex'")
    public List<Launch> fetchUpcomingLaunches() {
        String url = baseUrl + "/launches/upcoming";
        List<Launch> launches = new ArrayList<>();
        try {
            JsonNode root = get(url);
            if (root.isArray()) {
                root.forEach(item -> {
                    String dateUtcStr = item.path("date_utc").asText("");
                    Instant windowStart = null;
                    if (!dateUtcStr.isEmpty()) {
                        try { windowStart = Instant.parse(dateUtcStr); }
                        catch (Exception ignored) {}
                    }

                    // Determine status
                    boolean tbd = item.path("date_precision").asText("").equalsIgnoreCase("tbd")
                               || item.path("tbd").asBoolean(false);

                    launches.add(Launch.builder()
                            .id(item.path("id").asText())
                            .name(item.path("name").asText("Unknown"))
                            .agency("SpaceX")
                            .agencyCode("SPACEX")
                            .rocket(item.path("rocket").asText("Falcon 9"))
                            .launchSite(extractLaunchPad(item))
                            .launchSiteCountry("USA")
                            .windowStart(windowStart)
                            .status(tbd ? "TBD" : "Go")
                            .missionDesc(extractMissionDesc(item))
                            .imageUrl(item.path("links").path("patch").path("small").asText(""))
                            .build());
                });

                // Sort by launch date, nulls last
                launches.sort((a, b) -> {
                    if (a.getWindowStart() == null) return 1;
                    if (b.getWindowStart() == null) return -1;
                    return a.getWindowStart().compareTo(b.getWindowStart());
                });
            }
        } catch (Exception e) {
            log.error("SpaceX launch fetch failed: {}", e.getMessage());
        }

        // Return only next 10
        return launches.size() > 10 ? launches.subList(0, 10) : launches;
    }

    private String extractLaunchPad(JsonNode item) {
        // SpaceX API returns launchpad ID — map common ones
        String padId = item.path("launchpad").asText("");
        return switch (padId) {
            case "5e9e4502f5090995de566f86" -> "LC-39A, KSC, Florida";
            case "5e9e4501f509092b78566f87" -> "SLC-40, CCSFS, Florida";
            case "5e9e4502f5090927ac566f77" -> "SLC-4E, Vandenberg, California";
            default -> "TBD";
        };
    }

    private String extractMissionDesc(JsonNode item) {
        JsonNode payloads = item.path("payloads");
        if (payloads.isArray() && payloads.size() > 0) {
            return "Payload: " + payloads.size() + " payload(s)";
        }
        return item.path("details").asText("Mission details TBD");
    }

    private JsonNode get(String url) throws Exception {
        Request req = new Request.Builder().url(url).get().build();
        try (Response res = httpClient.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new RuntimeException("HTTP " + res.code());
            return objectMapper.readTree(res.body().string());
        }
    }
}
