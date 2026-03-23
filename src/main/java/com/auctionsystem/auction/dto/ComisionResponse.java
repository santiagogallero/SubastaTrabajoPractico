package com.auctionsystem.auction.dto;

import java.math.BigDecimal;

public record ComisionResponse(
        BigDecimal importeFinal,
        BigDecimal porcentajeComprador,
        BigDecimal porcentajeVendedor,
        BigDecimal comisionComprador,
        BigDecimal comisionVendedor,
        BigDecimal totalPagaComprador,
        BigDecimal netoRecibeVendedor,
        BigDecimal ingresoCasaSubasta
) {
}
