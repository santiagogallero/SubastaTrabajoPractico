package com.auctionsystem.coleccion.dto;

import java.math.BigDecimal;

public record ProductoColeccionRequest(
        Integer productoId,
        BigDecimal precioBase
) {
}
