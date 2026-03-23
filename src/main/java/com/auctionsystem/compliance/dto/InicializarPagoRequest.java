package com.auctionsystem.compliance.dto;

import jakarta.validation.constraints.NotNull;

public record InicializarPagoRequest(
        @NotNull Integer registroSubastaId
) {
}
