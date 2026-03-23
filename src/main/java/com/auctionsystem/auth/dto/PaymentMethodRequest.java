package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentMethodRequest(
        @NotBlank String tipo,
        @NotBlank String aliasDescripcion,
        @NotBlank String moneda,
        BigDecimal montoGarantia,
        @NotNull Boolean verificado
) {
}
