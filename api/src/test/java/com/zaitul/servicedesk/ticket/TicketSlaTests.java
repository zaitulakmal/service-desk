package com.zaitul.servicedesk.ticket;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.common.ConflictException;
import com.zaitul.servicedesk.user.AppUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** SLA rules on the Ticket itself; plain Java, no Spring or database. */
class TicketSlaTests {

	private static final Instant START = Instant.parse("2026-10-20T09:00:00Z");

	private final AppUser requester = new AppUser("aina@example.com", "Aina Rahman");

	private final AppUser agent = new AppUser("daniel@example.com", "Daniel Lee");

	@Test
	void clockStartsWhenTicketIsCreated() {
		Ticket ticket = highPriorityTicket(false);

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
		assertThat(ticket.getSlaDueAt()).isEqualTo(START.plus(Duration.ofHours(8)));
		assertThat(ticket.slaState(START)).isEqualTo(SlaState.ON_TRACK);
	}

	@Test
	void becomesAtRiskWithLessThanAFifthOfTheTimeLeft() {
		Ticket ticket = highPriorityTicket(false);

		assertThat(ticket.slaState(START.plus(Duration.ofHours(6)))).isEqualTo(SlaState.ON_TRACK);
		assertThat(ticket.slaState(START.plus(Duration.ofMinutes(6 * 60 + 40)))).isEqualTo(SlaState.AT_RISK);
		assertThat(ticket.slaState(START.plus(Duration.ofHours(8)).plusSeconds(1))).isEqualTo(SlaState.BREACHED);
	}

	@Test
	void breachIsFlaggedOnlyOnce() {
		Ticket ticket = highPriorityTicket(false);
		Instant late = START.plus(Duration.ofHours(9));

		assertThat(ticket.markSlaBreached(START.plus(Duration.ofHours(1)))).isFalse();
		assertThat(ticket.markSlaBreached(late)).isTrue();
		assertThat(ticket.markSlaBreached(late.plusSeconds(60))).isFalse();
		assertThat(ticket.getSlaBreachedAt()).isEqualTo(late);
	}

	@Test
	void clockIsPausedWhileWaitingForApprovalAndResumesWithTheFullTimeLeft() {
		Ticket ticket = highPriorityTicket(true);
		Instant approvedAt = START.plus(Duration.ofDays(2));

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AWAITING_APPROVAL);
		assertThat(ticket.slaState(approvedAt)).isEqualTo(SlaState.PAUSED);
		assertThat(ticket.slaRemainingSeconds(approvedAt)).isEqualTo(Duration.ofHours(8).toSeconds());

		ticket.approvalGranted(approvedAt);

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
		assertThat(ticket.getSlaDueAt()).isEqualTo(approvedAt.plus(Duration.ofHours(8)));
	}

	@Test
	void approvedTicketThatWasAlreadyAssignedGoesStraightToInProgress() {
		Ticket ticket = highPriorityTicket(true);
		ticket.assignTo(this.agent, START.plusSeconds(60));

		ticket.approvalGranted(START.plusSeconds(120));

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
	}

	@Test
	void resolvedInTimeIsMetAndLateIsMissed() {
		Ticket onTime = highPriorityTicket(false);
		onTime.assignTo(this.agent, START.plusSeconds(60));
		onTime.changeStatus(TicketStatus.RESOLVED, START.plus(Duration.ofHours(2)));

		Ticket late = highPriorityTicket(false);
		late.assignTo(this.agent, START.plusSeconds(60));
		late.changeStatus(TicketStatus.RESOLVED, START.plus(Duration.ofHours(10)));

		assertThat(onTime.slaState(START.plus(Duration.ofDays(1)))).isEqualTo(SlaState.MET);
		assertThat(late.slaState(START.plus(Duration.ofDays(1)))).isEqualTo(SlaState.MISSED);
	}

	@Test
	void assigningRecordsTheFirstResponseAndStartsWork() {
		Ticket ticket = highPriorityTicket(false);
		Instant assignedAt = START.plus(Duration.ofMinutes(15));

		ticket.assignTo(this.agent, assignedAt);
		ticket.changeStatus(TicketStatus.RESOLVED, assignedAt.plusSeconds(600));

		assertThat(ticket.getFirstResponseAt()).isEqualTo(assignedAt);
	}

	@Test
	void rejectedTicketCannotBeAssigned() {
		Ticket ticket = highPriorityTicket(true);
		ticket.approvalRejected();

		assertThat(ticket.slaState(START)).isEqualTo(SlaState.NONE);
		assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> ticket.assignTo(this.agent, START));
	}

	private Ticket highPriorityTicket(boolean needsApproval) {
		return new Ticket("Laptop screen flickering", "Flickers when docked", TicketType.INCIDENT, TicketCategory.IT,
				TicketPriority.HIGH, null, this.requester, needsApproval, START);
	}

}
