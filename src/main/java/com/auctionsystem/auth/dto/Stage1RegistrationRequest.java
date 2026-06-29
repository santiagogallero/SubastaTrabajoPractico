package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record Stage1RegistrationRequest(
        @NotBlank @Email String email,
        @NotBlank String documento,
        @NotBlank String nombre,
        @NotBlank String domicilioLegal,
        @NotBlank String paisOrigen
) {
}
