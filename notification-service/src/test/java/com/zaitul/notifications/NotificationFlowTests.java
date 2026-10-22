package com.zaitul.notifications;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Event in on Kafka → email out to a real SMTP server, end to end. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificationFlowTests {

	private static final String TOPIC = "servicedesk.ticket-events";

	@Autowired
	private KafkaTemplate<String, String> kafka;

	@Autowired
	private ProcessedEventRepository processedEvents;

	@Autowired
	private SentNotificationRepository sentNotifications;

	@Value("${test.mailpit-url}")
	private String mailpitUrl;

	@Autowired
	private ConsumerFactory<String, String> consumerFactory;

	private final RestClient http = RestClient.create();

	@BeforeEach
	void emptyTheMailbox() {
		this.http.delete().uri(this.mailpitUrl + "/api/v1/messages").retrieve().toBodilessEntity();
	}

	@Test
	void agentPickingUpATicketEmailsTheRequester() throws Exception {
		UUID eventId = UUID.randomUUID();
		publish(assignedEvent(eventId, 1042));

		waitUntil(() -> mailboxSize() == 1);
		String messages = mailbox();
		assertThat(JsonPath.<String>read(messages, "$.messages[0].Subject")).isEqualTo("Daniel Lee is working on SD-1042");
		assertThat(JsonPath.<String>read(messages, "$.messages[0].To[0].Address")).isEqualTo("aina@example.com");
		assertThat(this.sentNotifications.findByTicketId(1042)).hasSize(1);
	}

	@Test
	void anEventDeliveredTwiceIsEmailedOnce() throws Exception {
		UUID eventId = UUID.randomUUID();
		String event = assignedEvent(eventId, 2001);
		publish(event);
		publish(event);

		waitUntil(() -> this.processedEvents.existsById(eventId) && mailboxSize() >= 1);
		Thread.sleep(2000);
		assertThat(mailboxSize()).isEqualTo(1);
		assertThat(this.sentNotifications.findByTicketId(2001)).hasSize(1);
	}

	@Test
	void malformedEventIsParkedOnTheDeadLetterTopic() throws Exception {
		String marker = "not json " + UUID.randomUUID();
		publish(marker);

		try (Consumer<String, String> consumer = this.consumerFactory.createConsumer("test-" + UUID.randomUUID(),
				null)) {
			consumer.subscribe(List.of(TOPIC + ".DLT"));
			boolean found = false;
			long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
			while (!found && System.nanoTime() < deadline) {
				for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
					found |= marker.equals(record.value());
				}
			}
			assertThat(found).as("malformed event on the dead-letter topic").isTrue();
		}
	}

	private void publish(String payload) throws Exception {
		this.kafka.send(TOPIC, "1042", payload).get();
	}

	private int mailboxSize() {
		return JsonPath.<Integer>read(mailbox(), "$.total");
	}

	private String mailbox() {
		return this.http.get().uri(this.mailpitUrl + "/api/v1/messages").retrieve().body(String.class);
	}

	private static String assignedEvent(UUID eventId, long ticketId) {
		return """
				{ "eventId": "%s", "type": "TICKET_ASSIGNED", "occurredAt": "2026-10-20T09:00:00Z",
				  "ticket": { "id": %d, "reference": "SD-%d", "title": "Laptop screen flickering", "type": "INCIDENT",
				              "category": "IT", "priority": "HIGH", "status": "IN_PROGRESS" },
				  "actor": { "name": "Daniel Lee", "email": "daniel@example.com" },
				  "requester": { "name": "Aina Rahman", "email": "aina@example.com" },
				  "assignee": { "name": "Daniel Lee", "email": "daniel@example.com" },
				  "audience": [], "message": null, "someFieldAddedLater": true }
				""".formatted(eventId, ticketId, ticketId);
	}

	private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
		while (!condition.getAsBoolean()) {
			assertThat(System.nanoTime()).as("condition not met in time").isLessThan(deadline);
			Thread.sleep(200);
		}
	}

}
