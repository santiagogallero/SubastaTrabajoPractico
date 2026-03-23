package com.auctionsystem.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PujarRequest(
        @NotNull Integer subastaId,
        @NotNull Integer itemId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal importe,
        @NotBlank String moneda
) {
}
