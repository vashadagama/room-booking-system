package com.example.booking.security;

import com.example.booking.service.JwtService;

public record AuthenticatedUser(Long id, String username, String role) {

    public static final String ROLE_ADMIN = "ADMIN";

    public static AuthenticatedUser fromToken(JwtService jwtService, String token) {
        return new AuthenticatedUser(
                jwtService.extractUserId(token),
                jwtService.extractUsername(token),
                jwtService.extractRole(token));
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}