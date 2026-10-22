package com.zaitul.servicedesk.events;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import com.zaitul.servicedesk.events.TicketEvent.Person;
import com.zaitul.servicedesk.events.TicketEvent.TicketInfo;
import com.zaitul.servicedesk.ticket.Ticket;
import com.zaitul.servicedesk.user.AppUser;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records that something happened to a ticket. The event goes into the outbox table in
 * the caller's transaction; {@link OutboxRelay} sends it to Kafka afterwards.
 */
@Component
public class TicketEvents {

	private final OutboxEventRepository outbox;

	private final JsonMapper jsonMapper;

	private final Clock clock;

	TicketEvents(OutboxEventRepository outbox, JsonMapper jsonMapper, Clock clock) {
		this.outbox = outbox;
		this.jsonMapper = jsonMapper;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void publish(TicketEventType type, Ticket ticket, @Nullable AppUser actor) {
		publish(type, ticket, actor, List.of(), null);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void publish(TicketEventType type, Ticket ticket, @Nullable AppUser actor, List<AppUser> audience,
			@Nullable String message) {
		TicketEvent event = new TicketEvent(UUID.randomUUID(), type, this.clock.instant(), info(ticket),
				person(actor), person(ticket.getRequester()), person(ticket.getAssignee()),
				audience.stream().map(TicketEvents::person).toList(), message);
		String payload = this.jsonMapper.writeValueAsString(event);
		this.outbox.save(new OutboxEvent(event.eventId(), String.valueOf(ticket.getId()), type.name(), payload,
				event.occurredAt()));
	}

	private static TicketInfo info(Ticket ticket) {
		return new TicketInfo(ticket.getId(), ticket.getReference(), ticket.getTitle(), ticket.getType().name(),
				ticket.getCategory().name(), ticket.getPriority().name(), ticket.getStatus().name());
	}

	private static @Nullable Person person(@Nullable AppUser user) {
		return (user != null) ? new Person(user.getFullName(), user.getEmail()) : null;
	}

}
