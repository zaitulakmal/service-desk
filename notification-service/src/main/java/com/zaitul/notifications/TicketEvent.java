package com.zaitul.notifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The event the service desk API publishes. This is a copy of the API's contract on the
 * consumer side: services share a JSON format, never Java code. Unknown fields are
 * ignored, so the API can add fields without breaking this service.
 */
public record TicketEvent(UUID eventId, String type, Instant occurredAt, TicketInfo ticket, @Nullable Person actor,
		Person requester, @Nullable Person assignee, @Nullable List<Person> audience, @Nullable String message) {

	public List<Person> audienceOrEmpty() {
		return (this.audience != null) ? this.audience : List.of();
	}

	public record TicketInfo(long id, String reference, String title, String type, String category, String priority,
			String status) {
	}

	public record Person(String name, String email) {
	}

}
