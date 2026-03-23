package com.auctionsystem.auth.dto;

import java.util.Set;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        Set<String> roles
) {
}
