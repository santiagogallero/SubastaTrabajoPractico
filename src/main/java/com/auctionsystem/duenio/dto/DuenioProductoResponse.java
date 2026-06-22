package com.auctionsystem.duenio.dto;

import java.time.LocalDate;

public record DuenioProductoResponse(
        Integer id,
        String titulo,
        String descripcionCatalogo,
        String descripcionCompleta,
        String historia,
        boolean declaraPropiedad,
        boolean origenLicit,
        String estadoInspeccion,
        LocalDate fecha
) {}
