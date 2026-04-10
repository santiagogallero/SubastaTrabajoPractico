package com.auctionsystem.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RegisterPaymentMethodsRequest(
        @NotEmpty List<@Valid PaymentMethodRequest> mediosPago
) {
}
