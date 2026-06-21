package com.auctionsystem.auth.dto;

import java.math.BigDecimal;

public record MedioPagoResponse(
        Long id,
        String tipo,
        String aliasDescripcion,
        String moneda,
        BigDecimal montoGarantia,
        boolean verificado,
        boolean activo
) {
}
