package com.auctionsystem.seguro.dto;

import java.math.BigDecimal;

/**
 * Poliza de seguro asociada a un articulo del postor.
 */
public record SeguroPolizaResponse(
        Integer productoId,
        String productoTitulo,
        String nroPoliza,
        String compania,
        BigDecimal importeAsegurado,
        boolean polizaCombinada,
        String contactoTelefono,
        String contactoEmail
) {
}
