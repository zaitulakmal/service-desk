package com.zaitul.notifications;

import java.time.Clock;
import java.time.Instant;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationRules rules;

	private final JavaMailSender mailSender;

	private final ProcessedEventRepository processedEvents;

	private final SentNotificationRepository sentNotifications;

	private final Clock clock;

	private final String from;

	private final MeterRegistry registry;

	NotificationService(NotificationRules rules, JavaMailSender mailSender, ProcessedEventRepository processedEvents,
			SentNotificationRepository sentNotifications, Clock clock, @Value("${notifications.from}") String from,
			MeterRegistry registry) {
		this.registry = registry;
		// Start every counter at zero so Prometheus' increase() also counts the first email.
		for (String event : NotificationRules.EVENT_TYPES) {
			registry.counter("servicedesk.notifications.sent", "event", event);
		}
		registry.counter("servicedesk.notifications.duplicates");
		this.rules = rules;
		this.mailSender = mailSender;
		this.processedEvents = processedEvents;
		this.sentNotifications = sentNotifications;
		this.clock = clock;
		this.from = from;
	}

	/**
	 * Sends the emails for an event, at most once per event id. If sending fails, the
	 * exception reaches the Kafka error handler, which retries and finally parks the
	 * event on the dead-letter topic.
	 */
	@Transactional
	public void handle(TicketEvent event) {
		if (this.processedEvents.existsById(event.eventId())) {
			logger.info("Skipping {} {}: already handled", event.type(), event.eventId());
			this.registry.counter("servicedesk.notifications.duplicates").increment();
			return;
		}
		Instant now = this.clock.instant();
		this.processedEvents.save(new ProcessedEvent(event.eventId(), event.type(), now));
		for (Email email : this.rules.emailsFor(event)) {
			send(email);
			this.sentNotifications.save(
					new SentNotification(event.eventId(), event.ticket().id(), email.to(), email.subject(), now));
			this.registry.counter("servicedesk.notifications.sent", "event", event.type()).increment();
		}
	}

	private void send(Email email) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(this.from);
		message.setTo(email.to());
		message.setSubject(email.subject());
		message.setText(email.body());
		this.mailSender.send(message);
	}

}
