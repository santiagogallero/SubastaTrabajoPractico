package com.auctionsystem.auction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfigurarMonedaSubastaRequest(
        @NotNull Integer subastaId,
        @NotBlank String moneda
) {
}
