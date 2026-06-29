package com.auctionsystem.auction.dto;

import java.time.LocalDateTime;

public record SubastaTimingResponse(
        Integer subastaId,
        Integer duracionMinutos,
        java.time.LocalDateTime inicio,
        java.time.LocalDateTime fin,
        String estadoTemporal,
        Long minutosRestantes,
        Integer itemActualId,
        java.time.LocalDateTime itemExpiraAt,
        Long segundosRestantesItem
) {
}
