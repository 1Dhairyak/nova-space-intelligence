package com.nova.dto.auth;

public record AuthResponse(
        String token,
        String refreshToken,
        UserDto user
) {}
