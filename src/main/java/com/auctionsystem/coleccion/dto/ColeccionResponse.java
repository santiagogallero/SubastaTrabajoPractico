package com.auctionsystem.coleccion.dto;

import java.util.List;

public record ColeccionResponse(
        Integer id,
        String nombre,
        Integer duenioId,
        String duenioNombre,
        Integer subastaId,
        List<Integer> productoIds,
        int cantidadPiezas
) {
}
