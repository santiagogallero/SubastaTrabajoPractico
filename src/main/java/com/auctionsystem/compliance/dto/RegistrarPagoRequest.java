package com.auctionsystem.compliance.dto;

import jakarta.validation.constraints.NotNull;

public record RegistrarPagoRequest(
        @NotNull Integer registroSubastaId
) {
}
