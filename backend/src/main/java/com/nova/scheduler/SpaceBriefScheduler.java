package com.nova.scheduler;

import com.nova.model.SpaceBriefCache;
import com.nova.model.SpaceBriefCache.*;
import com.nova.service.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * All scheduled data-fetching jobs live here.
 *
 * Topology:
 *   ┌─────────────────┐   fetch    ┌──────────────┐   push    ┌───────────────┐
 *   │  External APIs  │ ─────────► │  Redis Cache │ ────────► │  WebSocket    │
 *   │  NASA/SpaceX    │            │  (TTL by job)│           │  /topic/...   │
 *   │  ISRO/NOAA/ISS  │            └──────────────┘           └───────────────┘
 *   └─────────────────┘
 *
 * Each job writes to Redis AND broadcasts the new data via STOMP so
 * connected React clients update in real time without polling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceBriefScheduler {

    private final NasaApiService      nasaApiService;
    private final SpaceXApiService    spaceXApiService;
    private final IsroApiService      isroApiService;
    private final IssTrackerService   issTrackerService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate         messagingTemplate;

    // Redis keys
    private static final String KEY_ISS      = "nova:iss";
    private static final String KEY_BRIEF    = "nova:brief";
    private static final String KEY_LAUNCHES = "nova:launches";
    private static final String KEY_NEO      = "nova:neo";
    private static final String KEY_WEATHER  = "nova:weather";

    // WebSocket topics
    private static final String TOPIC_ISS      = "/topic/iss";
    private static final String TOPIC_LAUNCHES = "/topic/launches";
    private static final String TOPIC_NEO      = "/topic/neo";
    private static final String TOPIC_WEATHER  = "/topic/weather";
    private static final String TOPIC_BRIEF    = "/topic/brief";

    // ─── ISS Position — every 5 seconds ──────────────────────────────────────

    @Scheduled(fixedRateString = "${scheduler.iss-interval}")
    public void updateIssPosition() {
        try {
            IssPosition pos = issTrackerService.fetchIssPosition();
            if (pos == null) return;

            redisTemplate.opsForValue().set(KEY_ISS, pos, Duration.ofSeconds(10));
            messagingTemplate.convertAndSend(TOPIC_ISS, pos);
            log.debug("ISS update pushed: {},{}", pos.getLatitude(), pos.getLongitude());
        } catch (Exception e) {
            log.error("ISS scheduler error: {}", e.getMessage());
        }
    }

    // ─── Space Weather (Kp + Flares) — every 5 minutes ───────────────────────

    @Scheduled(fixedRateString = "${scheduler.space-weather-interval}")
    public void updateSpaceWeather() {
        try {
            List<SpaceBriefCache.SpaceWeather.SolarFlare> flares = nasaApiService.fetchSolarFlares();
            SpaceWeather weather    = issTrackerService.buildSpaceWeather(flares);

            redisTemplate.opsForValue().set(KEY_WEATHER, weather, Duration.ofMinutes(6));
            messagingTemplate.convertAndSend(TOPIC_WEATHER, weather);
            log.info("Space weather updated — Kp: {}, flares: {}",
                    weather.getKpIndex(), flares.size());
        } catch (Exception e) {
            log.error("Space weather scheduler error: {}", e.getMessage());
        }
    }

    // ─── Launches (SpaceX + ISRO + NASA) — every 5 minutes ───────────────────

    @Scheduled(fixedRateString = "${scheduler.launch-interval}")
    public void updateLaunches() {
        try {
            List<Launch> all = new ArrayList<>();
            all.addAll(spaceXApiService.fetchUpcomingLaunches());
            all.addAll(isroApiService.fetchIsroUpcomingLaunches());
            all.addAll(isroApiService.fetchNasaUpcomingLaunches());

            // Sort by window start, nulls last
            all.sort((a, b) -> {
                if (a.getWindowStart() == null) return 1;
                if (b.getWindowStart() == null) return -1;
                return a.getWindowStart().compareTo(b.getWindowStart());
            });

            redisTemplate.opsForValue().set(KEY_LAUNCHES, all, Duration.ofMinutes(6));
            messagingTemplate.convertAndSend(TOPIC_LAUNCHES, all);
            log.info("Launches updated — total: {} (SpaceX + ISRO + NASA)", all.size());
        } catch (Exception e) {
            log.error("Launch scheduler error: {}", e.getMessage());
        }
    }

    // ─── NEO Feed — every 1 hour ──────────────────────────────────────────────

    @Scheduled(fixedRateString = "${scheduler.neo-interval}")
    public void updateNeo() {
        try {
            List<NearEarthObject> neos = nasaApiService.fetchNeoFeed();
            redisTemplate.opsForValue().set(KEY_NEO, neos, Duration.ofHours(2));
            messagingTemplate.convertAndSend(TOPIC_NEO, neos);
            log.info("NEO feed updated — {} objects", neos.size());
        } catch (Exception e) {
            log.error("NEO scheduler error: {}", e.getMessage());
        }
    }

    // ─── Full Brief Assembly — every 5 minutes ────────────────────────────────

    /**
     * Assembles all data into a single SpaceBriefCache object.
     * This is what gets injected into the Claude API system prompt
     * as the live-data context for Nova's responses.
     */
    @Scheduled(fixedRateString = "${scheduler.launch-interval}")
    public void assembleBrief() {
        try {
            Object issRaw  = redisTemplate.opsForValue().get(KEY_ISS);
            Object wxRaw   = redisTemplate.opsForValue().get(KEY_WEATHER);
            Object launchRaw = redisTemplate.opsForValue().get(KEY_LAUNCHES);
            Object neoRaw  = redisTemplate.opsForValue().get(KEY_NEO);

            SpaceBriefCache brief = SpaceBriefCache.builder()
                    .fetchedAt(Instant.now())
                    .issPosition(issRaw instanceof IssPosition iss ? iss : null)
                    .spaceWeather(wxRaw instanceof SpaceWeather wx ? wx : null)
                    .upcomingLaunches(launchRaw instanceof List<?> l
                            ? (List<Launch>) l : List.of())
                    .nearEarthObjects(neoRaw instanceof List<?> n
                            ? (List<NearEarthObject>) n : List.of())
                    .apod(nasaApiService.fetchApod())
                    .build();

            redisTemplate.opsForValue().set(KEY_BRIEF, brief, Duration.ofMinutes(6));
            messagingTemplate.convertAndSend(TOPIC_BRIEF, brief);
            log.info("Full brief assembled and broadcast");
        } catch (Exception e) {
            log.error("Brief assembly error: {}", e.getMessage());
        }
    }
}
