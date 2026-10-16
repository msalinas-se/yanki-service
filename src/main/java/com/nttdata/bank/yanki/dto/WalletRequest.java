package com.nttdata.bank.yanki.dto;

import com.nttdata.bank.yanki.model.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload used to create or update a wallet. */
public record WalletRequest(
    @NotNull DocumentType documentType,
    @NotBlank String documentNumber,
    @NotBlank String phone,
    @NotBlank String imei,
    @NotBlank @Email String email) {
}
