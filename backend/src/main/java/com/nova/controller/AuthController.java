package com.nova.controller;

import com.nova.dto.auth.*;
import com.nova.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Body: { "email": "...", "password": "...", "displayName": "..." }
     * Returns: { "token": "...", "refreshToken": "...", "user": {...} }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * POST /api/auth/login
     * Body: { "email": "...", "password": "..." }
     * Returns: { "token": "...", "refreshToken": "...", "user": {...} }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/refresh
     * Body: { "refreshToken": "..." }
     * Returns: { "token": "...", "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * GET /api/auth/me
     * Requires valid JWT in Authorization header.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @RequestAttribute("authenticatedUser") UserDto user) {
        return ResponseEntity.ok(user);
    }
}
