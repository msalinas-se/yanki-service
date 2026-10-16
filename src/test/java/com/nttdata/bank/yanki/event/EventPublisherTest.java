package com.nttdata.bank.yanki.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {
  private static final String JSON =
      "{\"walletId\":\"w1\",\"cardNumber\":\"5000\",\"success\":true,\"reason\":null}";

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  @Test
  void publishEvent() {
    CompletableFuture<SendResult<String, String>> sent = CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send("wallet-link-result", "w1", JSON)).thenReturn(sent);

    new EventPublisher(kafkaTemplate, new ObjectMapper())
        .publish("wallet-link-result", "w1", new WalletLinkResultEvent("w1", "5000", true, null))
        .test()
        .assertComplete();
    verify(kafkaTemplate).send("wallet-link-result", "w1", JSON);
  }
}
