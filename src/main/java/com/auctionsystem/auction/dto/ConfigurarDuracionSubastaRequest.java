package com.auctionsystem.auction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigurarDuracionSubastaRequest(
        @NotNull Integer subastaId,
        @NotNull @Min(1) Integer duracionMinutos
) {
}
