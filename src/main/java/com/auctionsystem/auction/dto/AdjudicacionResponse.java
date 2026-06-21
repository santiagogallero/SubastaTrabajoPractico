package com.auctionsystem.auction.dto;

import java.math.BigDecimal;

public record AdjudicacionResponse(
        Integer registroId,
        Integer itemId,
        Integer productoId,
        String productoDescripcion,
        Integer ganadorClienteId,
        String ganadorNombre,
        BigDecimal importe,
        BigDecimal comision,
        BigDecimal costoEnvio,
        BigDecimal totalPagar,
        String moneda,
        String estado,
        String mensaje,
        String modalidadEntrega,
        String direccionEnvio,
        Boolean seguroVigenteTrasEntrega
) {
}
