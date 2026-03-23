package com.auctionsystem.auction.dto;

import java.math.BigDecimal;

public record PujaResponse(
        Integer pujoId,
        Integer itemId,
        BigDecimal ofertaAnterior,
        BigDecimal ofertaActual,
        BigDecimal minimoPermitido,
        BigDecimal maximoPermitido,
        String mensaje
) {
}
