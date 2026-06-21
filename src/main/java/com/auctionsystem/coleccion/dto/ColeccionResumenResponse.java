package com.auctionsystem.coleccion.dto;

public record ColeccionResumenResponse(
        Integer id,
        String nombre,
        Integer subastaId,
        int cantidadPiezas,
        String duenioNombre
) {
}
