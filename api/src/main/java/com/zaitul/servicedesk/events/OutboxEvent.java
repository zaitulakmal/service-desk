package com.zaitul.servicedesk.events;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

	@Id
	private UUID id;

	/** The ticket id. Used as the Kafka key so all events of one ticket stay in order. */
	@Column(nullable = false, length = 64)
	private String aggregateId;

	@Column(nullable = false, length = 60)
	private String type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String payload;

	@Column(nullable = false)
	private Instant createdAt;

	private @Nullable Instant publishedAt;

	protected OutboxEvent() {
		// required by JPA
	}

	OutboxEvent(UUID id, String aggregateId, String type, String payload, Instant createdAt) {
		this.id = id;
		this.aggregateId = aggregateId;
		this.type = type;
		this.payload = payload;
		this.createdAt = createdAt;
	}

	void markPublished(Instant now) {
		this.publishedAt = now;
	}

	public UUID getId() {
		return this.id;
	}

	public String getAggregateId() {
		return this.aggregateId;
	}

	public String getType() {
		return this.type;
	}

	public String getPayload() {
		return this.payload;
	}

	public @Nullable Instant getPublishedAt() {
		return this.publishedAt;
	}

}
