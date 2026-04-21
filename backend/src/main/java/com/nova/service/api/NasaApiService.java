package com.nova.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.model.SpaceBriefCache.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NasaApiService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${nasa.api-key}")
    private String apiKey;

    @Value("${nasa.base-url}")
    private String baseUrl;

    // ─── APOD ────────────────────────────────────────────────────────────────

    /**
     * Fetches NASA's Astronomy Picture of the Day.
     * Cached for 12 hours — NASA updates once per day.
     */
    @Cacheable(value = "apod", key = "'today'")
    public Apod fetchApod() {
        String url = baseUrl + "/planetary/apod?api_key=" + apiKey;
        try {
            JsonNode node = get(url);
            return Apod.builder()
                    .title(node.path("title").asText())
                    .explanation(node.path("explanation").asText())
                    .url(node.path("url").asText())
                    .hdUrl(node.path("hdurl").asText(""))
                    .mediaType(node.path("media_type").asText("image"))
                    .date(node.path("date").asText())
                    .copyright(node.path("copyright").asText("NASA"))
                    .build();
        } catch (Exception e) {
            log.error("APOD fetch failed: {}", e.getMessage());
            return Apod.builder().title("Unavailable").explanation("").build();
        }
    }

    // ─── NeoWs ───────────────────────────────────────────────────────────────

    /**
     * Fetches Near Earth Objects for the next 7 days.
     * NASA NeoWs returns a feed grouped by date; we flatten it.
     * Cached for 1 hour.
     */
    @Cacheable(value = "neo", key = "'feed'")
    public List<NearEarthObject> fetchNeoFeed() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String end   = LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_DATE);
        String url   = baseUrl + "/neo/rest/v1/feed?start_date=" + today
                     + "&end_date=" + end + "&api_key=" + apiKey;
        List<NearEarthObject> result = new ArrayList<>();
        try {
            JsonNode root = get(url);
            JsonNode dates = root.path("near_earth_objects");
            dates.fields().forEachRemaining(entry -> {
                entry.getValue().forEach(neo -> {
                    JsonNode approach = neo.path("close_approach_data").get(0);
                    JsonNode diameter = neo.path("estimated_diameter").path("kilometers");
                    result.add(NearEarthObject.builder()
                            .id(neo.path("id").asText())
                            .name(neo.path("name").asText())
                            .distanceAu(approach.path("miss_distance").path("astronomical").asDouble())
                            .distanceKm(approach.path("miss_distance").path("kilometers").asDouble())
                            .diameterMinKm(diameter.path("estimated_diameter_min").asDouble())
                            .diameterMaxKm(diameter.path("estimated_diameter_max").asDouble())
                            .relativeVelocityKms(approach.path("relative_velocity")
                                    .path("kilometers_per_second").asDouble())
                            .potentiallyHazardous(neo.path("is_potentially_hazardous_asteroid").asBoolean())
                            .closeApproachDate(Instant.parse(
                                    approach.path("close_approach_date_full").asText()
                                            .replace(" ", "T") + ":00Z"))
                            .build());
                });
            });
            // Sort by distance ascending — closest first
            result.sort((a, b) -> Double.compare(a.getDistanceAu(), b.getDistanceAu()));
        } catch (Exception e) {
            log.error("NEO fetch failed: {}", e.getMessage());
        }
        return result;
    }

    // ─── DONKI Solar Flares ──────────────────────────────────────────────────

    /**
     * Fetches solar flare events from NASA DONKI for the past 7 days.
     * Cached for 5 minutes.
     */
    @Cacheable(value = "weather", key = "'flares'")
    public List<SpaceWeather.SolarFlare> fetchSolarFlares() {
        String start = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_DATE);
        String end   = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String url   = baseUrl + "/DONKI/FLR?startDate=" + start
                     + "&endDate=" + end + "&api_key=" + apiKey;
        List<SpaceWeather.SolarFlare> flares = new ArrayList<>();
        try {
            JsonNode root = get(url);
            if (root.isArray()) {
                root.forEach(flare -> {
                    Instant endTime = null;
                    String endStr = flare.path("endTime").asText("");
                    if (!endStr.isEmpty()) {
                        try { endTime = Instant.parse(endStr.replace(" ", "T") + ":00Z"); }
                        catch (Exception ignored) {}
                    }
                    Instant beginTime = parseNasaTime(flare.path("beginTime").asText(""));
                    Instant peakTime  = parseNasaTime(flare.path("peakTime").asText(""));

                    flares.add(SpaceWeather.SolarFlare.builder()
                            .classType(flare.path("classType").asText("Unknown"))
                            .beginTime(beginTime)
                            .peakTime(peakTime)
                            .endTime(endTime)
                            .status(endTime == null ? "ACTIVE" : "ENDED")
                            .region(flare.path("activeRegionNum").asText("N/A"))
                            .build());
                });
            }
        } catch (Exception e) {
            log.error("Solar flare fetch failed: {}", e.getMessage());
        }
        return flares;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private JsonNode get(String url) throws Exception {
        Request req = new Request.Builder().url(url).get().build();
        try (Response res = httpClient.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new RuntimeException("HTTP " + res.code());
            return objectMapper.readTree(res.body().string());
        }
    }

    private Instant parseNasaTime(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Instant.parse(s.replace(" ", "T") + ":00Z"); }
        catch (Exception e) { return null; }
    }
}
