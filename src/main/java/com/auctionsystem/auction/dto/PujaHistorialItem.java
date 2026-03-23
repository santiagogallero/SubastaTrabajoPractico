package com.auctionsystem.auction.dto;

import java.math.BigDecimal;

public record PujaHistorialItem(
        Integer pujoId,
        Integer asistenteId,
        Integer numeroPostor,
        Integer clienteId,
        String nombreCliente,
        BigDecimal importe,
        String ganador
) {
}
