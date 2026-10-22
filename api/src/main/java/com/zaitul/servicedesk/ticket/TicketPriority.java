package com.zaitul.servicedesk.ticket;

import java.time.Duration;

/**
 * Priority sets the SLA: how long the support team has to resolve the ticket.
 * Measured in calendar time (not business hours) to keep the rules easy to follow.
 */
public enum TicketPriority {

	LOW(Duration.ofDays(5)),

	MEDIUM(Duration.ofDays(3)),

	HIGH(Duration.ofHours(8)),

	URGENT(Duration.ofHours(4));

	private final Duration slaTarget;

	TicketPriority(Duration slaTarget) {
		this.slaTarget = slaTarget;
	}

	public Duration getSlaTarget() {
		return this.slaTarget;
	}

}
