package com.nttdata.bank.yanki.event;

import java.math.BigDecimal;

/** Payment that must be applied on the main accounts of the linked debit cards. */
public record WalletPaymentEvent(String paymentId, BigDecimal amount, String fromCardNumber,
    String toCardNumber) {
}
