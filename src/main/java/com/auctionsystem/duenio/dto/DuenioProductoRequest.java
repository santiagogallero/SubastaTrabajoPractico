package com.auctionsystem.duenio.dto;

import jakarta.validation.constraints.NotBlank;

public record DuenioProductoRequest(
        @NotBlank String titulo,
        String descripcionCatalogo,
        @NotBlank String descripcionCompleta,
        String historia,
        boolean declaraPropiedad,
        boolean origenLicit
) {}
