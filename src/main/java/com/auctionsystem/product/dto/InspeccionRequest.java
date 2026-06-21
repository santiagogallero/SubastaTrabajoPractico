package com.auctionsystem.product.dto;

/**
 * Decision de un empleado al inspeccionar un articulo.
 *
 * @param aprobado true si se acepta para subastar, false si se rechaza.
 * @param motivo   motivo del rechazo (obligatorio cuando aprobado == false).
 */
public record InspeccionRequest(
        boolean aprobado,
        String motivo
) {
}
