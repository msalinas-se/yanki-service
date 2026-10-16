package com.nttdata.bank.yanki.exception;

/** Error body returned by the API. */
public record ErrorResponse(String code, String message) {
}
