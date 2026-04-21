package com.nova.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.model.SpaceBriefCache;
import com.nova.model.SpaceBriefCache.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Middleware between the React frontend and Anthropic Claude API.
 *
 * Flow:
 *  1. Client sends message via WebSocket to /app/nova/chat
 *  2. NovaController calls NovaAiService.chat(userId, message)
 *  3. This service fetches the latest SpaceBriefCache from Redis
 *  4. Builds a rich system prompt with all live space data
 *  5. Calls Claude API (claude-sonnet-4-20250514) via OkHttp
 *  6. Streams the response back to /user/{userId}/queue/nova
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovaAiService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.base-url}")
    private String anthropicBaseUrl;

    private static final String KEY_BRIEF = "nova:brief";
    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    /**
     * Sends a user message to Claude with live space context injected,
     * then pushes the response back to the user's personal WebSocket queue.
     */
    public void chat(String userId, String userMessage, String conversationId) {
        SpaceBriefCache brief = getBrief();
        String systemPrompt   = buildSystemPrompt(brief);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", 512,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            ));
        } catch (Exception e) {
            log.error("Failed to serialize Claude request: {}", e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(anthropicBaseUrl + "/messages")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("x-api-key", anthropicApiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Claude API error: {}", response.code());
                pushNovaMessage(userId, conversationId,
                        "Signal interrupted. Status: " + response.code());
                return;
            }
            JsonNode body = objectMapper.readTree(response.body().string());
            String reply  = body.path("content").get(0).path("text").asText();
            pushNovaMessage(userId, conversationId, reply);
        } catch (Exception e) {
            log.error("Claude call failed: {}", e.getMessage());
            pushNovaMessage(userId, conversationId, "Uplink lost. Retrying…");
        }
    }

    private void pushNovaMessage(String userId, String conversationId, String text) {
        Map<String, String> payload = Map.of(
                "conversationId", conversationId,
                "role", "nova",
                "text", text
        );
        messagingTemplate.convertAndSendToUser(userId, "/queue/nova", payload);
    }

    // ─── System Prompt Builder ────────────────────────────────────────────────

    private String buildSystemPrompt(SpaceBriefCache brief) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are Nova, an advanced AI space intelligence system integrated into a
                real-time space operations dashboard. You are concise, precise, and authoritative
                — like a mission control officer briefing astronauts. You have access to live
                telemetry data. Keep responses under 80 words unless the user explicitly asks
                for detail. Use specific numbers when available. Never say "I don't have
                real-time data" — all the data below is live.
                
                """);

        if (brief == null) {
            sb.append("DATA STATUS: Initialising — brief not yet assembled.\n");
            return sb.toString();
        }

        sb.append("=== LIVE SPACE DATA (as of ").append(UTC_FMT.format(brief.getFetchedAt()))
          .append(" UTC) ===\n\n");

        // ISS
        IssPosition iss = brief.getIssPosition();
        if (iss != null) {
            sb.append(String.format(
                    "ISS: lat %.2f°, lon %.2f°, altitude %.0f km, speed %.2f km/s\n",
                    iss.getLatitude(), iss.getLongitude(),
                    iss.getAltitudeKm(), iss.getSpeedKms()));
        }

        // Space weather
        SpaceWeather wx = brief.getSpaceWeather();
        if (wx != null) {
            sb.append(String.format("Geomagnetic Kp index: %.1f (Storm level: %s)\n",
                    wx.getKpIndex(), wx.getStormLevel()));
            if (wx.getFlares() != null && !wx.getFlares().isEmpty()) {
                long activeFlares = wx.getFlares().stream()
                        .filter(f -> "ACTIVE".equals(f.getStatus())).count();
                sb.append(String.format("Solar flares today: %d total, %d active\n",
                        wx.getFlares().size(), activeFlares));
                wx.getFlares().stream()
                        .filter(f -> "ACTIVE".equals(f.getStatus()))
                        .forEach(f -> sb.append(String.format(
                                "  → %s class flare active since %s UTC\n",
                                f.getClassType(),
                                f.getBeginTime() != null ? UTC_FMT.format(f.getBeginTime()) : "N/A")));
            }
        }

        // Launches
        List<Launch> launches = brief.getUpcomingLaunches();
        if (launches != null && !launches.isEmpty()) {
            sb.append("\nUpcoming launches (next 3):\n");
            launches.stream().limit(3).forEach(l -> sb.append(String.format(
                    "  → [%s] %s | %s | %s | Window: %s UTC\n",
                    l.getAgencyCode(), l.getName(), l.getRocket(), l.getLaunchSite(),
                    l.getWindowStart() != null ? UTC_FMT.format(l.getWindowStart()) : "TBD")));
        }

        // NEOs
        List<NearEarthObject> neos = brief.getNearEarthObjects();
        if (neos != null && !neos.isEmpty()) {
            long hazardous = neos.stream().filter(NearEarthObject::isPotentiallyHazardous).count();
            sb.append(String.format("\nNear-Earth Objects this week: %d total, %d potentially hazardous\n",
                    neos.size(), hazardous));
            neos.stream().limit(3).forEach(neo -> sb.append(String.format(
                    "  → %s: %.4f AU (%.0f km), diam %.2f–%.2f km%s\n",
                    neo.getName(), neo.getDistanceAu(), neo.getDistanceKm(),
                    neo.getDiameterMinKm(), neo.getDiameterMaxKm(),
                    neo.isPotentiallyHazardous() ? " ⚠ POTENTIALLY HAZARDOUS" : "")));
        }

        // APOD
        Apod apod = brief.getApod();
        if (apod != null && apod.getTitle() != null && !apod.getTitle().isEmpty()) {
            sb.append(String.format("\nAPOD today: \"%s\" — %s\n",
                    apod.getTitle(), apod.getDate()));
        }

        return sb.toString();
    }

    private SpaceBriefCache getBrief() {
        try {
            Object raw = redisTemplate.opsForValue().get(KEY_BRIEF);
            if (raw instanceof SpaceBriefCache b) return b;
        } catch (Exception e) {
            log.warn("Could not read brief from Redis: {}", e.getMessage());
        }
        return null;
    }
}
