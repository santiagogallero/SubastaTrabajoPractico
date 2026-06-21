package com.auctionsystem.compliance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EjecutarPagoResponse(
        boolean exito,
        String transaccionId,
        BigDecimal montoPagado,
        String moneda,
        String productoDescripcion,
        String medioPagoAlias,
        LocalDateTime fechaPago
) {
}
