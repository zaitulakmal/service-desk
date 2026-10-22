package com.zaitul.notifications;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

	@Id
	private UUID eventId;

	@Column(nullable = false, length = 60)
	private String type;

	@Column(nullable = false)
	private Instant processedAt;

	protected ProcessedEvent() {
		// required by JPA
	}

	ProcessedEvent(UUID eventId, String type, Instant processedAt) {
		this.eventId = eventId;
		this.type = type;
		this.processedAt = processedAt;
	}

	public UUID getEventId() {
		return this.eventId;
	}

}
