package com.auctionsystem.product.dto;

import com.auctionsystem.entities.Producto;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vista de un articulo (producto) para el flujo de venta e inspeccion.
 */
public record ArticuloResponse(
        Integer id,
        String titulo,
        String categoria,
        String descripcionCatalogo,
        String descripcionCompleta,
        String historia,
        String estadoInspeccion,
        String motivoRechazo,
        boolean disponible,
        LocalDate fecha,
        LocalDateTime fechaInspeccion,
        String duenioNombre,
        String revisorNombre,
        long cantidadFotos,
        boolean declaraPropiedad,
        boolean origenLicit
) {
    public static ArticuloResponse de(Producto p, long cantidadFotos) {
        String duenioNombre = p.getDuenio() != null && p.getDuenio().getPersona() != null
                ? p.getDuenio().getPersona().getNombre()
                : null;
        String revisorNombre = p.getRevisor() != null && p.getRevisor().getPersona() != null
                ? p.getRevisor().getPersona().getNombre()
                : null;
        return new ArticuloResponse(
                p.getId(),
                p.getTitulo(),
                p.getCategoria(),
                p.getDescripcionCatalogo(),
                p.getDescripcionCompleta(),
                p.getHistoria(),
                p.getEstadoInspeccion(),
                p.getMotivoRechazo(),
                "si".equalsIgnoreCase(p.getDisponible()),
                p.getFecha(),
                p.getFechaInspeccion(),
                duenioNombre,
                revisorNombre,
                cantidadFotos,
                p.isDeclaraPropiedad(),
                p.isOrigenLicit()
        );
    }
}
