package com.auctionsystem.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record Stage2RegistrationRequest(
        @NotBlank String categoria,
        @NotEmpty List<@Valid PaymentMethodRequest> mediosPago
) {
}
