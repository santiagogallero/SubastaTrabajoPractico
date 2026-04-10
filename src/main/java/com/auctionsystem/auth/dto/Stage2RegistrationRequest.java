package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record Stage2RegistrationRequest(
        @NotBlank String numeroTramite,
        @NotBlank String docFrenteUrl,
        @NotBlank String docDorsoUrl
) {
}
