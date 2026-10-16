package com.nttdata.bank.yanki.event;

/** Request to link a wallet to a debit card of the bank. */
public record WalletLinkEvent(String walletId, String cardNumber) {
}
