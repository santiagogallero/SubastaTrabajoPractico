package com.auctionsystem.auth.dto;

import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String email,
        String estado,
        Integer personaId,
        Set<String> roles,
        String categoria
) {
}