package com.nova.controller;

import com.nova.service.NovaAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for Nova AI chat.
 *
 * Client sends to:   /app/nova/chat
 * Nova replies to:   /user/{username}/queue/nova
 *
 * Message format:
 *   { "message": "What's the ISS doing?", "conversationId": "uuid" }
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NovaChatController {

    private final NovaAiService novaAiService;

    @MessageMapping("/nova/chat")
    public void handleChat(@Payload Map<String, String> payload,
                           Principal principal) {
        String userId         = principal != null ? principal.getName() : "anonymous";
        String userMessage    = payload.getOrDefault("message", "");
        String conversationId = payload.getOrDefault("conversationId",
                UUID.randomUUID().toString());

        if (userMessage.isBlank()) return;

        log.debug("Nova chat from {}: {}", userId, userMessage);

        // Run in a virtual thread so the WebSocket thread is never blocked
        Thread.ofVirtual().start(() ->
                novaAiService.chat(userId, userMessage, conversationId));
    }
}
