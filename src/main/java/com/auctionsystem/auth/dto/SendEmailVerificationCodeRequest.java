package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailVerificationCodeRequest(
        @NotBlank @Email String email
) {
}
