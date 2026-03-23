package com.auctionsystem.compliance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoEstadoDto(
        Integer registroSubastaId,
        String estadoPago,
        BigDecimal montoOfertado,
        BigDecimal montoMulta,
        LocalDateTime fechaVencimiento,
        LocalDateTime fechaLimiteRegularizacion,
        boolean bloqueado
) {
}
