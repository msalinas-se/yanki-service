package com.nttdata.bank.yanki.exception;

import lombok.Getter;

/** Raised when an operation breaks a business rule. */
@Getter
public class BusinessRuleException extends RuntimeException {
  private final String code;

  /** Creates the exception with an error code and a message. */
  public BusinessRuleException(String code, String message) {
    super(message);
    this.code = code;
  }
}
