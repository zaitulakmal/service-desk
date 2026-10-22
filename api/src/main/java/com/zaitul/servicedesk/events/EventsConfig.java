package com.zaitul.servicedesk.events;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "servicedesk.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
class EventsConfig {

	/** Created on startup if missing. 3 partitions lets up to 3 consumers share the load. */
	@Bean
	NewTopic ticketEventsTopic(@Value("${servicedesk.events.topic}") String topic,
			@Value("${servicedesk.events.replicas:1}") int replicas) {
		return TopicBuilder.name(topic).partitions(3).replicas(replicas).build();
	}

}
