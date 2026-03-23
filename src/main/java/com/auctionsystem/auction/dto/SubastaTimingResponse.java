package com.auctionsystem.auction.dto;

import java.time.LocalDateTime;

public record SubastaTimingResponse(
        Integer subastaId,
        Integer duracionMinutos,
        LocalDateTime inicio,
        LocalDateTime fin,
        String estadoTemporal,
        Long minutosRestantes
) {
}
