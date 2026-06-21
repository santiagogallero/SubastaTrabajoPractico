package com.auctionsystem.product.dto;

import java.util.List;

/**
 * Datos que envia un postor para poner a la venta un articulo propio. El
 * articulo queda en estado PENDIENTE hasta que un empleado lo inspecciona y
 * decide aprobarlo o rechazarlo.
 *
 * @param titulo             titulo corto del articulo (para el catalogo).
 * @param categoria          categoria declarada (Arte, Joyeria, etc.).
 * @param descripcionCompleta descripcion tecnica completa.
 * @param historia           procedencia / origen / historia declarada.
 * @param fotos              imagenes en base64 (admite prefijo data URI, minimo 6).
 * @param declaraPropiedad   el postor declara ser dueno legitimo del articulo.
 * @param origenLicit        el postor declara origen licito del articulo.
 */
public record PublicarArticuloRequest(
        String titulo,
        String categoria,
        String descripcionCompleta,
        String historia,
        List<String> fotos,
        Boolean declaraPropiedad,
        Boolean origenLicit
) {
}
