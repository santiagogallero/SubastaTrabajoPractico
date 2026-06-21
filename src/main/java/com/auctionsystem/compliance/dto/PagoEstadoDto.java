package com.auctionsystem.compliance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoEstadoDto(
        Integer registroSubastaId,
        String estadoPago,
        BigDecimal montoOfertado,
        BigDecimal montoTotal,
        BigDecimal montoMulta,
        BigDecimal multaPotencial,
        String moneda,
        String productoDescripcion,
        String transaccionId,
        LocalDateTime fechaVencimiento,
        LocalDateTime fechaLimiteRegularizacion,
        boolean bloqueado
) {
}
