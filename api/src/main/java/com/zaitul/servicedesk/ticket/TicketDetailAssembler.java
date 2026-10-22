package com.zaitul.servicedesk.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.zaitul.servicedesk.approval.ApprovalStatus;
import com.zaitul.servicedesk.approval.ApprovalStep;
import com.zaitul.servicedesk.approval.ApprovalStepRepository;
import com.zaitul.servicedesk.attachment.TicketAttachmentRepository;
import com.zaitul.servicedesk.ticket.TicketDetailResponse.ActivityView;
import com.zaitul.servicedesk.ticket.TicketDetailResponse.ApprovalStepView;
import com.zaitul.servicedesk.ticket.TicketDetailResponse.AttachmentView;
import com.zaitul.servicedesk.ticket.TicketDetailResponse.CommentView;
import com.zaitul.servicedesk.ticket.TicketDetailResponse.Permissions;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.CurrentUser;
import com.zaitul.servicedesk.user.UserSummary;

import org.springframework.stereotype.Component;

/** Builds the ticket page response. Call inside a transaction (it reads lazy relations). */
@Component
public class TicketDetailAssembler {

	private final ApprovalStepRepository approvalSteps;

	private final TicketCommentRepository comments;

	private final TicketActivityRepository activities;

	private final TicketAttachmentRepository attachments;

	private final TicketAccess access;

	private final Clock clock;

	TicketDetailAssembler(ApprovalStepRepository approvalSteps, TicketCommentRepository comments,
			TicketActivityRepository activities, TicketAttachmentRepository attachments, TicketAccess access,
			Clock clock) {
		this.approvalSteps = approvalSteps;
		this.comments = comments;
		this.activities = activities;
		this.attachments = attachments;
		this.access = access;
		this.clock = clock;
	}

	public TicketDetailResponse toDetail(Ticket ticket, CurrentUser me) {
		return toDetail(ticket, me, this.approvalSteps.findForTicket(ticket.getId()));
	}

	TicketDetailResponse toDetail(Ticket ticket, CurrentUser me, List<ApprovalStep> chain) {
		Instant now = this.clock.instant();
		boolean canWork = this.access.canWork(ticket, me);
		boolean canDecide = !me.is(ticket.getRequester())
				&& chain.stream().anyMatch((step) -> step.canBeDecidedBy(me));
		Permissions permissions = new Permissions(
				canWork && !ticket.getStatus().isFinished() && ticket.getStatus() != TicketStatus.RESOLVED
						&& (ticket.getAssignee() == null || !me.is(ticket.getAssignee())),
				canWork ? ticket.getStatus().allowedNext() : List.of(),
				this.access.canComment(ticket, me, chain), canDecide);
		return new TicketDetailResponse(ticket.getId(), ticket.getReference(), ticket.getTitle(),
				ticket.getDescription(), ticket.getType(), ticket.getCategory(), ticket.getPriority(),
				ticket.getStatus(), ticket.getPurchaseAmount(), UserSummary.of(ticket.getRequester()),
				ticket.getRequester().getDepartment(), UserSummary.ofNullable(ticket.getAssignee()),
				SlaInfo.of(ticket, now), ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getFirstResponseAt(),
				ticket.getResolvedAt(), chain.stream().map(TicketDetailAssembler::toView).toList(),
				this.comments.findForTicket(ticket.getId())
					.stream()
					.map((c) -> new CommentView(c.getId(), UserSummary.of(c.getAuthor()), c.getBody(),
							c.getCreatedAt()))
					.toList(),
				this.attachments.findForTicket(ticket.getId())
					.stream()
					.map((a) -> new AttachmentView(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(),
							a.getUploadedBy().getFullName(), a.getCreatedAt()))
					.toList(),
				this.activities.findForTicket(ticket.getId())
					.stream()
					.map((a) -> new ActivityView(a.getType(), a.getMessage(), a.getCreatedAt()))
					.toList(),
				permissions);
	}

	private static ApprovalStepView toView(ApprovalStep step) {
		AppUser decidedBy = step.getDecidedBy();
		return new ApprovalStepView(step.getId(), step.getStepOrder(), step.getName(), step.getApproverName(),
				step.getStatus(), (step.getStatus() == ApprovalStatus.PENDING) ? step.getPendingSince() : null,
				(decidedBy != null) ? decidedBy.getFullName() : null, step.getDecidedAt(), step.getComment());
	}

}
