package com.zaitul.servicedesk.dashboard;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.ticket.TicketCategory;
import com.zaitul.servicedesk.ticket.TicketSummaryResponse;

/**
 * @param openRequests tickets OPEN or IN_PROGRESS right now
 * @param slaBreached tickets that breached their SLA within the period
 * @param averageFirstResponseSeconds for tickets created within the period, or empty if none
 */
public record DashboardResponse(int periodDays, long openRequests, long awaitingApproval, long slaBreached,
		@Nullable Long averageFirstResponseSeconds, List<CategoryCount> openByCategory,
		List<TicketSummaryResponse> needsAttention, List<WaitingApproval> approvalsWaitingLongest) {

	public record CategoryCount(TicketCategory category, long count) {
	}

	public record WaitingApproval(long ticketId, String reference, String title, String waitingOn, Instant since) {
	}

}
