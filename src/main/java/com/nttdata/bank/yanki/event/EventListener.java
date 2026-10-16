package com.nttdata.bank.yanki.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bank.yanki.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes the answers of the bank. */
@Component
@RequiredArgsConstructor
public class EventListener {
  private final WalletService walletService;
  private final ObjectMapper objectMapper;

  /** Receives the result of linking a debit card. */
  @SneakyThrows
  @KafkaListener(topics = "${topics.wallet-link-result}", groupId = "yanki-service")
  public void onLinkResult(String message) {
    walletService.onLinkResult(objectMapper.readValue(message, WalletLinkResultEvent.class))
        .blockingAwait();
  }

  /** Receives the result of a payment made with debit cards. */
  @SneakyThrows
  @KafkaListener(topics = "${topics.wallet-payment-result}", groupId = "yanki-service")
  public void onPaymentResult(String message) {
    walletService.onPaymentResult(objectMapper.readValue(message, WalletPaymentResultEvent.class))
        .blockingAwait();
  }
}
