package com.auctionsystem.catalog.dto;

import java.math.BigDecimal;

/**
 * Item del catalogo de una subasta. {@code precioBase} es {@code null} para
 * visitantes no autenticados (consigna: el precio base solo lo ven registrados).
 */
public record CatalogoItemResponse(
        Integer itemId,
        Integer productoId,
        String titulo,
        String descripcion,
        String categoria,
        BigDecimal precioBase,
        String moneda,
        boolean tieneSeguro,
        String duenioNombre
) {
}
