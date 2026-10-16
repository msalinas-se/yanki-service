package com.nttdata.bank.yanki.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes events in Kafka as JSON. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  /** Sends an event to a topic. */
  public Completable publish(String topic, String key, Object event) {
    return Completable.defer(() -> {
      String json;
      try {
        json = objectMapper.writeValueAsString(event);
      } catch (JsonProcessingException e) {
        return Completable.error(e);
      }
      return Completable.fromCompletionStage(kafkaTemplate.send(topic, key, json))
          .doOnComplete(() -> log.info("Event sent to {}: {}", topic, event));
    });
  }
}
