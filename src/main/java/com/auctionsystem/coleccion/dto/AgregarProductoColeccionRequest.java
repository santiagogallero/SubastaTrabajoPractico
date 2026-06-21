package com.auctionsystem.coleccion.dto;

import java.math.BigDecimal;

public record AgregarProductoColeccionRequest(
        Integer productoId,
        BigDecimal precioBase
) {
}
