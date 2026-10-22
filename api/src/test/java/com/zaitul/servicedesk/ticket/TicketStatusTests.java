package com.zaitul.servicedesk.ticket;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTests {

	@ParameterizedTest(name = "{0} -> {1} allowed: {2}")
	@CsvSource({
			"OPEN,              IN_PROGRESS, true",
			"OPEN,              CLOSED,      true",
			"OPEN,              RESOLVED,    false",
			"OPEN,              OPEN,        false",
			"IN_PROGRESS,       RESOLVED,    true",
			"IN_PROGRESS,       OPEN,        false",
			"IN_PROGRESS,       CLOSED,      false",
			"RESOLVED,          CLOSED,      true",
			"RESOLVED,          IN_PROGRESS, true",
			"RESOLVED,          OPEN,        false",
			"CLOSED,            OPEN,        false",
			"CLOSED,            IN_PROGRESS, false",
			"AWAITING_APPROVAL, OPEN,        false",
			"AWAITING_APPROVAL, CLOSED,      false",
			"REJECTED,          OPEN,        false" })
	void agentsFollowTheTicketLifecycle(TicketStatus from, TicketStatus to, boolean allowed) {
		assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
	}

}
