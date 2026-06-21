package com.auctionsystem.payout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CuentaCobroRequest(
        @NotBlank @Size(max = 120) String alias,
        @NotBlank @Size(max = 10) String currency,
        Boolean foreignAccount,
        @NotBlank @Size(max = 120) String bankName,
        @NotBlank @Size(max = 64) String accountNumber,
        @Size(max = 32) String swiftCode,
        @NotBlank @Size(max = 120) String holderName
) {
}
