package com.zaitul.servicedesk.approval;

import java.time.Instant;

import com.zaitul.servicedesk.ticket.TicketType;
import com.zaitul.servicedesk.user.UserSummary;

public record PendingApprovalResponse(long ticketId, String reference, String title, TicketType type,
		UserSummary requester, String stepName, Instant pendingSince) {
}
