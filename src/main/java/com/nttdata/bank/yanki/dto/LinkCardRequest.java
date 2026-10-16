package com.nttdata.bank.yanki.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload used to link a wallet to a debit card. */
public record LinkCardRequest(@NotBlank String cardNumber) {
}
