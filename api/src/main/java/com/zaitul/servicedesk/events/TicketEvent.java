package com.zaitul.servicedesk.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * What the API publishes to Kafka. It carries everything a consumer needs (names,
 * emails) so consumers never have to call back into the API ("event-carried state").
 * This JSON shape is a contract: add fields freely, but never rename or remove one.
 *
 * @param audience extra people this event is for: the approvers for APPROVAL_REQUESTED,
 * the service managers for SLA_BREACHED
 * @param message free text that depends on the type, e.g. the comment or the new status
 */
public record TicketEvent(UUID eventId, TicketEventType type, Instant occurredAt, TicketInfo ticket,
		@Nullable Person actor, Person requester, @Nullable Person assignee, List<Person> audience,
		@Nullable String message) {

	public record TicketInfo(long id, String reference, String title, String type, String category,
			String priority, String status) {
	}

	public record Person(String name, String email) {
	}

}
