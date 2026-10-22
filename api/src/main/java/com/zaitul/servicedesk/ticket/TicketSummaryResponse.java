package com.zaitul.servicedesk.ticket;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.user.UserSummary;

/** One row in a ticket list. */
public record TicketSummaryResponse(long id, String reference, String title, TicketType type,
		TicketCategory category, TicketPriority priority, TicketStatus status, UserSummary requester,
		@Nullable UserSummary assignee, SlaInfo sla, @Nullable LatestUpdate latestUpdate, Instant createdAt,
		Instant updatedAt) {

	public static TicketSummaryResponse of(Ticket ticket, @Nullable TicketActivity latest, Instant now) {
		return new TicketSummaryResponse(ticket.getId(), ticket.getReference(), ticket.getTitle(), ticket.getType(),
				ticket.getCategory(), ticket.getPriority(), ticket.getStatus(), UserSummary.of(ticket.getRequester()),
				UserSummary.ofNullable(ticket.getAssignee()), SlaInfo.of(ticket, now),
				(latest != null) ? new LatestUpdate(latest.getMessage(), latest.getCreatedAt()) : null,
				ticket.getCreatedAt(), ticket.getUpdatedAt());
	}

	public record LatestUpdate(String message, Instant at) {
	}

}
