package com.auctionsystem.metrics;

import java.math.BigDecimal;

public record MetricasUsuarioResponse(
        String categoria,
        long subastasParticipadas,
        long pujasRealizadas,
        long subastasGanadas,
        BigDecimal totalOfertado,
        BigDecimal totalPagado
) {
}
