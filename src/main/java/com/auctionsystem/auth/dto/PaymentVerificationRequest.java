package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentVerificationRequest(
        @NotNull Long medioPagoId,
        @NotNull Boolean verificado
) {
}
