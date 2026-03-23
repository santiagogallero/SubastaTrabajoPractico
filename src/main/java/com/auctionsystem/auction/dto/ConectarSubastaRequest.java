package com.auctionsystem.auction.dto;

import jakarta.validation.constraints.NotNull;

public record ConectarSubastaRequest(
        @NotNull Integer subastaId
) {
}
