package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record Stage1RegistrationRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String documento,
        @NotBlank String numeroTramite,
        @NotBlank String nombre,
        @NotBlank String domicilioLegal,
        @NotBlank String paisOrigen,
        @NotBlank String docFrenteUrl,
        @NotBlank String docDorsoUrl
) {
}
