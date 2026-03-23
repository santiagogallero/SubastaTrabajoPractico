package com.auctionsystem.chat.dto;

import java.time.LocalDateTime;

public record MensajeChatDto(
        Long id,
        Long conversacionId,
        Long remitenteUsuarioId,
        String remitenteEmail,
        String texto,
        LocalDateTime enviadoAt
) {
}
