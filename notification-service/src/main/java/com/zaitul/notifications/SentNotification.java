package com.zaitul.notifications;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification")
public class SentNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private UUID eventId;

	@Column(nullable = false)
	private long ticketId;

	@Column(nullable = false, length = 320)
	private String recipient;

	@Column(nullable = false, length = 300)
	private String subject;

	@Column(nullable = false)
	private Instant sentAt;

	protected SentNotification() {
		// required by JPA
	}

	SentNotification(UUID eventId, long ticketId, String recipient, String subject, Instant sentAt) {
		this.eventId = eventId;
		this.ticketId = ticketId;
		this.recipient = recipient;
		this.subject = subject;
		this.sentAt = sentAt;
	}

	public String getRecipient() {
		return this.recipient;
	}

	public String getSubject() {
		return this.subject;
	}

}
