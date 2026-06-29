package com.auctionsystem.auction.dto;

import java.math.BigDecimal;
import java.util.List;

public record CierreSubastaResponse(
        Integer subastaId,
        int itemsVendidos,
        int itemsSinOfertas,
        List<ItemCierreDto> detalle
) {
    public record ItemCierreDto(
            Integer itemId,
            String descripcionProducto,
            BigDecimal precioBase,
            BigDecimal importeGanador,
            String emailGanador,
            String nombreGanador,
            String resultado  // VENDIDO | SIN_OFERTAS
    ) {}
}
