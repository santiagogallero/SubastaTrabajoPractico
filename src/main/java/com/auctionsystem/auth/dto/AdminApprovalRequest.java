package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminApprovalRequest(
        @NotBlank String email,
        @NotBlank String categoria
) {
}
