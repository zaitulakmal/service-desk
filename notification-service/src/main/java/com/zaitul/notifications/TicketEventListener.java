package com.zaitul.notifications;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class TicketEventListener {

	private final JsonMapper jsonMapper;

	private final NotificationService notificationService;

	TicketEventListener(JsonMapper jsonMapper, NotificationService notificationService) {
		this.jsonMapper = jsonMapper;
		this.notificationService = notificationService;
	}

	@KafkaListener(topics = "${notifications.topic}")
	void onTicketEvent(String payload) {
		TicketEvent event = this.jsonMapper.readValue(payload, TicketEvent.class);
		this.notificationService.handle(event);
	}

}
