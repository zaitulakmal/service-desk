package com.zaitul.servicedesk.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import com.zaitul.servicedesk.events.OutboxEventRepository;
import com.zaitul.servicedesk.ticket.Ticket;
import com.zaitul.servicedesk.ticket.TicketCategory;
import com.zaitul.servicedesk.ticket.TicketType;

import org.springframework.stereotype.Component;

/**
 * Business metrics, scraped by Prometheus from /actuator/prometheus alongside the
 * technical ones (HTTP latency, JVM memory, Kafka client) that Spring Boot adds itself.
 */
@Component
public class ServiceDeskMetrics {

	/** Named "raised", not "created": Prometheus reserves the _created suffix for its own use. */
	private static final String TICKETS_RAISED = "servicedesk.tickets.raised";

	private static final String SLA_BREACHES = "servicedesk.sla.breaches";

	private static final String APPROVALS_DECIDED = "servicedesk.approvals.decided";

	private final MeterRegistry registry;

	ServiceDeskMetrics(MeterRegistry registry, OutboxEventRepository outbox) {
		this.registry = registry;
		// If this keeps growing, Kafka is down or the relay is stuck.
		Gauge.builder("servicedesk.outbox.pending", outbox, OutboxEventRepository::countByPublishedAtIsNull)
			.description("Events saved but not yet published to Kafka")
			.register(registry);
		// Register every counter at zero on startup. A counter that only appears at its
		// first increment makes Prometheus' increase() miss that first event.
		for (TicketCategory category : TicketCategory.values()) {
			registry.counter(SLA_BREACHES, "category", category.name());
			for (TicketType type : TicketType.values()) {
				registry.counter(TICKETS_RAISED, "category", category.name(), "type", type.name());
			}
		}
		registry.counter(APPROVALS_DECIDED, "decision", "approved");
		registry.counter(APPROVALS_DECIDED, "decision", "rejected");
	}

	public void ticketCreated(Ticket ticket) {
		this.registry
			.counter(TICKETS_RAISED, "category", ticket.getCategory().name(), "type", ticket.getType().name())
			.increment();
	}

	public void slaBreached(Ticket ticket) {
		this.registry.counter(SLA_BREACHES, "category", ticket.getCategory().name()).increment();
	}

	public void approvalDecided(boolean approved) {
		this.registry.counter(APPROVALS_DECIDED, "decision", approved ? "approved" : "rejected").increment();
	}

}
