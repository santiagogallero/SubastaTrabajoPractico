package com.auctionsystem.coleccion.dto;

import java.util.List;

public record CrearColeccionRequest(
        String nombre,
        Integer duenioId,
        Integer subastaId,
        List<ProductoColeccionRequest> productos
) {
}
