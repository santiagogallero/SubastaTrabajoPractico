package com.auctionsystem.chat.dto;

import java.time.LocalDateTime;

public record ConversacionDto(
        Long id,
        Long duenioUsuarioId,
        Long empleadoUsuarioId,
        String estado,
        LocalDateTime updatedAt
) {
}
