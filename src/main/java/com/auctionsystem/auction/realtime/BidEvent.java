package com.auctionsystem.auction.realtime;

import java.math.BigDecimal;

/**
 * Evento que se difunde por WebSocket a todos los postores conectados a una
 * subasta cuando se registra una nueva puja. Permite la actualizacion en
 * tiempo real de la "oferta actual" sin necesidad de refrescar manualmente.
 */
public record BidEvent(
        String tipo,
        Integer subastaId,
        Integer itemId,
        Integer pujoId,
        Integer numeroPostor,
        String nombrePostor,
        BigDecimal importe,
        BigDecimal minimoPermitido,
        BigDecimal maximoPermitido,
        String timestamp
) {
    public static final String TIPO_NUEVA_PUJA = "NUEVA_PUJA";
}
