package com.auctionsystem.catalog.dto;

import java.util.List;

public record CatalogoResponse(
        Integer subastaId,
        Integer catalogoId,
        String descripcion,
        String moneda,
        List<CatalogoItemResponse> items
) {
}
