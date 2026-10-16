package com.nttdata.bank.yanki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Payload used to send money from one phone to another. */
public record PaymentRequest(@NotBlank String fromPhone, @NotBlank String toPhone,
    @NotNull @Positive BigDecimal amount) {
}
