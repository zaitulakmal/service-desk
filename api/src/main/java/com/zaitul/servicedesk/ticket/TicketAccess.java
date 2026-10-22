package com.zaitul.servicedesk.ticket;

import java.util.List;

import com.zaitul.servicedesk.approval.ApprovalStep;
import com.zaitul.servicedesk.user.CurrentUser;
import com.zaitul.servicedesk.user.Roles;

import org.springframework.stereotype.Component;

/** Who may see and change a ticket. */
@Component
public class TicketAccess {

	/**
	 * The requester, the assignee, agents of the ticket's team, service managers and
	 * anyone in its approval chain. Everyone else gets 404, so they cannot even tell
	 * the ticket exists.
	 */
	public boolean canView(Ticket ticket, CurrentUser me, List<ApprovalStep> approvalChain) {
		return me.is(ticket.getRequester()) || (ticket.getAssignee() != null && me.is(ticket.getAssignee()))
				|| me.isAgentFor(ticket.getCategory()) || me.hasRole(Roles.SERVICE_MANAGER)
				|| approvalChain.stream().anyMatch((step) -> step.involves(me));
	}

	/** Assigning and changing status is for agents of the ticket's team. */
	public boolean canWork(Ticket ticket, CurrentUser me) {
		return me.isAgentFor(ticket.getCategory());
	}

	public boolean canComment(Ticket ticket, CurrentUser me, List<ApprovalStep> approvalChain) {
		return canView(ticket, me, approvalChain) && !ticket.getStatus().isFinished();
	}

}
