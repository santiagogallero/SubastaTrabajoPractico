package com.auctionsystem.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmDto(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "debe tener 6 digitos") String code,
        @NotBlank @Size(min = 8, message = "minimo 8 caracteres") String newPassword
) {
}
