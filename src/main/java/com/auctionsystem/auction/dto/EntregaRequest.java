package com.auctionsystem.auction.dto;

public record EntregaRequest(
        String deliveryOption,
        String shippingAddress
) {
}
