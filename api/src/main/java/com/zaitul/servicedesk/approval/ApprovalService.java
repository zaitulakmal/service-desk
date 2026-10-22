package com.zaitul.servicedesk.approval;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.zaitul.servicedesk.common.ConflictException;
import com.zaitul.servicedesk.common.InvalidFieldException;
import com.zaitul.servicedesk.events.TicketEventType;
import com.zaitul.servicedesk.events.TicketEvents;
import com.zaitul.servicedesk.metrics.ServiceDeskMetrics;
import com.zaitul.servicedesk.ticket.ActivityLog;
import com.zaitul.servicedesk.ticket.ActivityType;
import com.zaitul.servicedesk.ticket.Ticket;
import com.zaitul.servicedesk.ticket.TicketAccess;
import com.zaitul.servicedesk.ticket.TicketDetailAssembler;
import com.zaitul.servicedesk.ticket.TicketDetailResponse;
import com.zaitul.servicedesk.ticket.TicketNotFoundException;
import com.zaitul.servicedesk.ticket.TicketRepository;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.AppUserRepository;
import com.zaitul.servicedesk.user.CurrentUser;
import com.zaitul.servicedesk.user.UserSummary;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

	private final TicketRepository tickets;

	private final ApprovalStepRepository steps;

	private final AppUserRepository users;

	private final ApprovalWorkflow workflow;

	private final TicketAccess access;

	private final TicketDetailAssembler assembler;

	private final ActivityLog activityLog;

	private final TicketEvents events;

	private final ServiceDeskMetrics metrics;

	private final Clock clock;

	ApprovalService(TicketRepository tickets, ApprovalStepRepository steps, AppUserRepository users,
			ApprovalWorkflow workflow, TicketAccess access, TicketDetailAssembler assembler, ActivityLog activityLog,
			TicketEvents events, ServiceDeskMetrics metrics, Clock clock) {
		this.metrics = metrics;
		this.tickets = tickets;
		this.steps = steps;
		this.users = users;
		this.workflow = workflow;
		this.access = access;
		this.assembler = assembler;
		this.activityLog = activityLog;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	public TicketDetailResponse decide(long ticketId, ApprovalDecisionRequest request, CurrentUser me) {
		Ticket ticket = this.tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
		List<ApprovalStep> chain = this.steps.findForTicket(ticketId);
		if (!this.access.canView(ticket, me, chain)) {
			throw new TicketNotFoundException(ticketId);
		}
		ApprovalStep pending = chain.stream()
			.filter((step) -> step.getStatus() == ApprovalStatus.PENDING)
			.findFirst()
			.orElseThrow(() -> new ConflictException("This request is not waiting for an approval"));
		if (me.is(ticket.getRequester())) {
			throw new AccessDeniedException("You cannot approve your own request");
		}
		if (!pending.canBeDecidedBy(me)) {
			throw new AccessDeniedException("This approval is waiting on " + pending.getApproverName());
		}
		AppUser approver = this.users.findById(me.id()).orElseThrow();
		Instant now = this.clock.instant();
		if (request.decision() == ApprovalDecision.APPROVE) {
			approve(ticket, chain, pending, approver, request.comment(), now);
		}
		else {
			reject(ticket, pending, approver, request.comment(), now);
		}
		return this.assembler.toDetail(ticket, me);
	}

	private void approve(Ticket ticket, List<ApprovalStep> chain, ApprovalStep pending, AppUser approver,
			String comment, Instant now) {
		pending.approve(approver, comment, now);
		this.metrics.approvalDecided(true);
		this.activityLog.record(ticket, approver, ActivityType.APPROVED,
				approver.getFullName() + " approved (" + pending.getName() + ")");
		Optional<ApprovalStep> next = chain.stream()
			.filter((step) -> step.getStatus() == ApprovalStatus.WAITING)
			.findFirst();
		if (next.isPresent()) {
			next.get().activate(now);
			this.workflow.requestDecision(ticket, next.get(), approver);
			return;
		}
		ticket.approvalGranted(now);
		this.activityLog.record(ticket, null, ActivityType.SLA_RESUMED, "All approvals done. SLA resumed.");
		this.events.publish(TicketEventType.TICKET_APPROVED, ticket, approver, List.of(), comment);
	}

	private void reject(Ticket ticket, ApprovalStep pending, AppUser approver, String comment, Instant now) {
		if (comment == null || comment.isBlank()) {
			throw new InvalidFieldException("comment", "Give the requester a reason when rejecting");
		}
		pending.reject(approver, comment, now);
		this.metrics.approvalDecided(false);
		ticket.approvalRejected();
		this.activityLog.record(ticket, approver, ActivityType.REJECTED,
				approver.getFullName() + " rejected the request");
		this.events.publish(TicketEventType.TICKET_REJECTED, ticket, approver, List.of(), comment);
	}

	@Transactional(readOnly = true)
	public List<PendingApprovalResponse> pendingFor(CurrentUser me) {
		return this.steps.findPendingFor(me.id(), me.roles())
			.stream()
			.map((step) -> new PendingApprovalResponse(step.getTicket().getId(), step.getTicket().getReference(),
					step.getTicket().getTitle(), step.getTicket().getType(),
					UserSummary.of(step.getTicket().getRequester()), step.getName(), step.getPendingSince()))
			.toList();
	}

}
