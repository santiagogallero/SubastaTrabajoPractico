package com.auctionsystem.chat.dto;

import java.time.LocalDateTime;

public record ConversacionDto(
        Long id,
        Long duenioUsuarioId,
        String duenioEmail,
        String duenioNombre,
        Long empleadoUsuarioId,
        String empleadoEmail,
        String estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer productoId,
        String productoTitulo,
        String productoEstadoInspeccion,
        String productoMotivoRechazo,
        String primeraFotoBase64
) {
}
