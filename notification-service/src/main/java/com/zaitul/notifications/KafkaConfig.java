package com.zaitul.notifications;

import java.time.Clock;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import tools.jackson.core.JacksonException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
class KafkaConfig {

	static final String DEAD_LETTER_SUFFIX = ".DLT";

	/**
	 * A failing event is retried 3 times, 2 seconds apart (e.g. the mail server is down).
	 * If it still fails, it is parked on the dead-letter topic so it does not block the
	 * events behind it. Malformed JSON will never succeed, so it goes there straight away.
	 */
	@Bean
	DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
				(record, ex) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, -1));
		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000, 3));
		handler.addNotRetryableExceptions(JacksonException.class);
		return handler;
	}

	/** Same definition as the API's, so it does not matter which service starts first. */
	@Bean
	NewTopic ticketEventsTopic(@Value("${notifications.topic}") String topic) {
		return TopicBuilder.name(topic).partitions(3).replicas(1).build();
	}

	@Bean
	NewTopic deadLetterTopic(@Value("${notifications.topic}") String topic) {
		return TopicBuilder.name(topic + DEAD_LETTER_SUFFIX).partitions(1).replicas(1).build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
