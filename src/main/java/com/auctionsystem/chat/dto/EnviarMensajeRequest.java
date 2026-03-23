package com.auctionsystem.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensajeRequest(
        @NotBlank String texto
) {
}
