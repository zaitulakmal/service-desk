package com.zaitul.servicedesk.events;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.zaitul.servicedesk.ApiTestSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** The outbox relay running for real, against a real Kafka broker in Docker. */
@TestPropertySource(properties = { "servicedesk.outbox.relay.enabled=true",
		"servicedesk.outbox.relay.interval=PT0.2S" })
class OutboxRelayTests extends ApiTestSupport {

	private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.1"));

	static {
		kafka.start();
	}

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	@Autowired
	private OutboxEventRepository outbox;

	@Test
	void ticketEventsReachKafkaInOrderKeyedByTicket() throws Exception {
		long id = createIncident(this.aina);
		this.mockMvc.perform(post("/api/tickets/{id}/assign", id).with(as(this.daniel)));

		List<ConsumerRecord<String, String>> records = consume(2);

		assertThat(records).extracting(ConsumerRecord::key).containsOnly(String.valueOf(id));
		assertThat(records).extracting(OutboxRelayTests::eventType).containsExactly("TICKET_CREATED", "TICKET_ASSIGNED");
		String assigned = records.get(1).value();
		assertThat(JsonPath.<String>read(assigned, "$.ticket.reference")).isEqualTo("SD-0001");
		assertThat(JsonPath.<String>read(assigned, "$.assignee.email")).isEqualTo("daniel@example.com");
		waitUntil(() -> this.outbox.countByPublishedAtIsNull() == 0);
	}

	private List<ConsumerRecord<String, String>> consume(int expected) {
		Map<String, Object> config = Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
				ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(), ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
				"earliest", ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		List<ConsumerRecord<String, String>> received = new ArrayList<>();
		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
			consumer.subscribe(List.of("servicedesk.ticket-events"));
			long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
			while (received.size() < expected && System.nanoTime() < deadline) {
				consumer.poll(Duration.ofMillis(500)).forEach(received::add);
			}
		}
		return received;
	}

	private static String eventType(ConsumerRecord<String, String> record) {
		return new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
	}

	private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
		while (!condition.getAsBoolean()) {
			assertThat(System.nanoTime()).as("condition not met in time").isLessThan(deadline);
			Thread.sleep(100);
		}
	}

}
