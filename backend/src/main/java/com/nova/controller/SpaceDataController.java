package com.nova.controller;

import com.nova.model.SpaceBriefCache;
import com.nova.model.SpaceBriefCache.*;
import com.nova.service.api.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for space data.
 * These are used on initial page load; after that, WebSocket pushes updates.
 *
 * GET /api/space/brief       → full SpaceBriefCache
 * GET /api/space/iss         → current ISS position
 * GET /api/space/launches    → all upcoming launches (SpaceX + ISRO + NASA)
 * GET /api/space/neo         → NEO feed
 * GET /api/space/weather     → space weather + flares
 * GET /api/space/apod        → astronomy picture of the day
 */
@RestController
@RequestMapping("/api/space")
@RequiredArgsConstructor
public class SpaceDataController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NasaApiService  nasaApiService;
    private final SpaceXApiService spaceXApiService;
    private final IsroApiService  isroApiService;

    @GetMapping("/brief")
    public ResponseEntity<?> getBrief() {
        Object brief = redisTemplate.opsForValue().get("nova:brief");
        if (brief != null) return ResponseEntity.ok(brief);
        return ResponseEntity.ok(Map.of("status", "initialising"));
    }

    @GetMapping("/iss")
    public ResponseEntity<?> getIss() {
        Object iss = redisTemplate.opsForValue().get("nova:iss");
        if (iss != null) return ResponseEntity.ok(iss);
        return ResponseEntity.ok(Map.of("status", "unavailable"));
    }

    @GetMapping("/launches")
    public ResponseEntity<List<Launch>> getLaunches() {
        Object cached = redisTemplate.opsForValue().get("nova:launches");
        if (cached instanceof List<?> list) return ResponseEntity.ok((List<Launch>) list);

        // Cache miss — fetch synchronously
        List<Launch> all = new java.util.ArrayList<>();
        all.addAll(spaceXApiService.fetchUpcomingLaunches());
        all.addAll(isroApiService.fetchIsroUpcomingLaunches());
        all.addAll(isroApiService.fetchNasaUpcomingLaunches());
        return ResponseEntity.ok(all);
    }

    @GetMapping("/neo")
    public ResponseEntity<List<NearEarthObject>> getNeo() {
        Object cached = redisTemplate.opsForValue().get("nova:neo");
        if (cached instanceof List<?> list) return ResponseEntity.ok((List<NearEarthObject>) list);
        return ResponseEntity.ok(nasaApiService.fetchNeoFeed());
    }

    @GetMapping("/weather")
    public ResponseEntity<?> getWeather() {
        Object cached = redisTemplate.opsForValue().get("nova:weather");
        if (cached != null) return ResponseEntity.ok(cached);
        return ResponseEntity.ok(Map.of("status", "unavailable"));
    }

    @GetMapping("/apod")
    public ResponseEntity<Apod> getApod() {
        return ResponseEntity.ok(nasaApiService.fetchApod());
    }

    @GetMapping("/launches/isro")
    public ResponseEntity<List<Launch>> getIsroLaunches() {
        return ResponseEntity.ok(isroApiService.fetchIsroUpcomingLaunches());
    }

    @GetMapping("/launches/spacex")
    public ResponseEntity<List<Launch>> getSpaceXLaunches() {
        return ResponseEntity.ok(spaceXApiService.fetchUpcomingLaunches());
    }
}
