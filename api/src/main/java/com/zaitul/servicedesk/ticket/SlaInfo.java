package com.zaitul.servicedesk.ticket;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * SLA status worked out on the server, so every screen shows the same answer.
 *
 * @param remainingSeconds time left (negative once breached); while paused, the time
 * that will be left when the clock restarts
 */
public record SlaInfo(SlaState state, @Nullable Instant dueAt, @Nullable Long remainingSeconds, long targetSeconds,
		@Nullable Instant breachedAt) {

	static SlaInfo of(Ticket ticket, Instant now) {
		return new SlaInfo(ticket.slaState(now), ticket.getSlaDueAt(), ticket.slaRemainingSeconds(now),
				ticket.getSlaTargetSeconds(), ticket.getSlaBreachedAt());
	}

}
