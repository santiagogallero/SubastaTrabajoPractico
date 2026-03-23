package com.auctionsystem.verification;

public record VerificationResult(
        boolean approved,
        String source,
        String detail,
        String canonicalName,
        String canonicalCountry
) {
}
