package com.zaitul.servicedesk.ticket;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum TicketStatus {

	AWAITING_APPROVAL,
	OPEN,
	IN_PROGRESS,
	RESOLVED,
	CLOSED,
	REJECTED;

	/** Statuses where the support team still has work to do and the SLA clock runs. */
	public static final Set<TicketStatus> ACTIVE = Set.of(OPEN, IN_PROGRESS);

	/**
	 * Status changes an agent may make by hand. OPEN can be cancelled straight to CLOSED;
	 * a RESOLVED ticket can be reopened to IN_PROGRESS if the fix did not work. Leaving
	 * AWAITING_APPROVAL only happens through an approval decision.
	 */
	public boolean canTransitionTo(TicketStatus next) {
		return switch (this) {
			case OPEN -> next == IN_PROGRESS || next == CLOSED;
			case IN_PROGRESS -> next == RESOLVED;
			case RESOLVED -> next == CLOSED || next == IN_PROGRESS;
			case AWAITING_APPROVAL, CLOSED, REJECTED -> false;
		};
	}

	public List<TicketStatus> allowedNext() {
		return Arrays.stream(values()).filter(this::canTransitionTo).toList();
	}

	public boolean isFinished() {
		return this == CLOSED || this == REJECTED;
	}

}
