package com.auctionsystem.compliance.dto;

import jakarta.validation.constraints.NotNull;

public record EjecutarPagoRequest(
        @NotNull Integer registroSubastaId,
        @NotNull Long medioPagoId
) {
}
