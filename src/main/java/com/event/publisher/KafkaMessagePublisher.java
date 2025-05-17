package com.event.publisher;

import com.event.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, Event> kafkaTemplate;

    @Value("${kafka.topic.events}")
    private String topicName;

    @Value("${kafka.publish.timeout.seconds:10}")
    private long publishTimeoutSeconds;

    /**
     * Publishes event updates to Kafka with retry capability.
     *
     * @param event The event data to publish
     * @throws MessagePublishException if the message cannot be published after retries
     */
    @Override
    @Retryable(retryFor = {MessagePublishException.class}, maxAttempts = 5, backoff = @Backoff(delay = 500, multiplier = 1.5))
    public void publishEventUpdate(Event event) {
        Objects.requireNonNull(event, "Event cannot be null");
        Objects.requireNonNull(event.getEventId(), "Event ID cannot be null");

        log.debug("Publishing event update to Kafka: {}", event);

        try {
            CompletableFuture<SendResult<String, Event>> future = kafkaTemplate.send(topicName, event.getEventId(), event);

            SendResult<String, Event> result = future.get(publishTimeoutSeconds, TimeUnit.SECONDS);

            log.info("Published event update successfully: eventId={}, topic={}, partition={}, offset={}", event.getEventId(), result.getRecordMetadata()
                    .topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());

        } catch (Exception ex) {
            log.error("Failed to publish event update to Kafka: {}", ex.getMessage(), ex);
            throw new MessagePublishException("Failed to publish event update for " + event.getEventId(), ex);
        }
    }

    /**
     * Custom exception for message publishing failures.
     */
    public static class MessagePublishException extends RuntimeException {
        public MessagePublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}