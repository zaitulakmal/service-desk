package com.zaitul.servicedesk.approval;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.events.TicketEventType;
import com.zaitul.servicedesk.events.TicketEvents;
import com.zaitul.servicedesk.ticket.ActivityLog;
import com.zaitul.servicedesk.ticket.ActivityType;
import com.zaitul.servicedesk.ticket.Ticket;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.AppUserRepository;

import org.springframework.stereotype.Component;

/** Creates a ticket's approval chain and asks each approver in turn. */
@Component
public class ApprovalWorkflow {

	private final ApprovalStepRepository steps;

	private final AppUserRepository users;

	private final ActivityLog activityLog;

	private final TicketEvents events;

	ApprovalWorkflow(ApprovalStepRepository steps, AppUserRepository users, ActivityLog activityLog,
			TicketEvents events) {
		this.steps = steps;
		this.users = users;
		this.activityLog = activityLog;
		this.events = events;
	}

	public void start(Ticket ticket, List<ApprovalPolicy.PlannedStep> plan, Instant now) {
		List<ApprovalStep> created = new ArrayList<>();
		for (int i = 0; i < plan.size(); i++) {
			created.add(this.steps.save(new ApprovalStep(ticket, i + 1, plan.get(i), now)));
		}
		requestDecision(ticket, created.getFirst(), null);
	}

	void requestDecision(Ticket ticket, ApprovalStep step, @Nullable AppUser actor) {
		this.activityLog.record(ticket, null, ActivityType.APPROVAL_REQUESTED,
				"Waiting for " + step.getApproverName() + " to approve. SLA paused.");
		this.events.publish(TicketEventType.APPROVAL_REQUESTED, ticket, actor, approversFor(step), step.getName());
	}

	private List<AppUser> approversFor(ApprovalStep step) {
		AppUser approver = step.getApprover();
		return (approver != null) ? List.of(approver) : this.users.findAllByRole(step.getApproverRole());
	}

}
