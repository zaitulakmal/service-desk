package com.zaitul.servicedesk.events;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Moves events from the outbox table to Kafka. If sending fails, the transaction rolls
 * back and the batch is retried on the next run, so delivery is "at least once":
 * consumers must ignore an event they have already seen (they check the event id).
 */
@Component
@ConditionalOnProperty(name = "servicedesk.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
class OutboxRelay {

	private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);

	private static final int BATCH_SIZE = 50;

	private final OutboxEventRepository outbox;

	private final KafkaTemplate<String, String> kafka;

	private final TransactionTemplate transaction;

	private final Clock clock;

	private final String topic;

	OutboxRelay(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka, TransactionTemplate transaction,
			Clock clock, @Value("${servicedesk.events.topic}") String topic) {
		this.outbox = outbox;
		this.kafka = kafka;
		this.transaction = transaction;
		this.clock = clock;
		this.topic = topic;
	}

	@Scheduled(fixedDelayString = "${servicedesk.outbox.relay.interval:PT1S}")
	void relay() {
		try {
			Integer sent;
			do {
				sent = this.transaction.execute((status) -> relayBatch());
			}
			while (sent != null && sent == BATCH_SIZE);
		}
		catch (RuntimeException ex) {
			logger.warn("Could not publish outbox events, will retry: {}", ex.getMessage());
		}
	}

	private int relayBatch() {
		List<OutboxEvent> batch = this.outbox.lockUnpublished(BATCH_SIZE);
		for (OutboxEvent event : batch) {
			send(event);
			event.markPublished(this.clock.instant());
		}
		return batch.size();
	}

	private void send(OutboxEvent event) {
		ProducerRecord<String, String> record = new ProducerRecord<>(this.topic, event.getAggregateId(),
				event.getPayload());
		record.headers().add("eventType", event.getType().getBytes(StandardCharsets.UTF_8));
		record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
		try {
			this.kafka.send(record).get(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while publishing " + event.getId(), ex);
		}
		catch (ExecutionException | TimeoutException ex) {
			throw new IllegalStateException("Failed to publish " + event.getId(), ex);
		}
	}

}
