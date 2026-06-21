package com.auctionsystem.auction.dto;

public record StreamingResponse(
        Integer subastaId,
        String streamingUrl
) {
}
