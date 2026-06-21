package com.auctionsystem.payout.dto;

public record CuentaCobroResponse(
        Long id,
        String alias,
        String currency,
        boolean foreignAccount,
        String bankName,
        String accountNumberMasked,
        String swiftCode,
        String holderName,
        boolean active
) {
}
