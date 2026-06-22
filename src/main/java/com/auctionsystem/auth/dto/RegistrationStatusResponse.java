package com.auctionsystem.auth.dto;

public record RegistrationStatusResponse(
        String email,
        String usuarioEstado,
        String etapa,
        boolean requiereVerificacionEmail,
        boolean puedeCompletarDocumentacion
) {
}
