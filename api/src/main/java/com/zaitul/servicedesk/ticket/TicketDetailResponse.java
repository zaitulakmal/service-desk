package com.zaitul.servicedesk.ticket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.approval.ApprovalStatus;
import com.zaitul.servicedesk.user.UserSummary;

/**
 * Everything the ticket page needs in one response, including what the signed-in user
 * is allowed to do, so the web app only shows buttons that will work.
 */
public record TicketDetailResponse(long id, String reference, String title, String description, TicketType type,
		TicketCategory category, TicketPriority priority, TicketStatus status, @Nullable BigDecimal purchaseAmount,
		UserSummary requester, @Nullable String requesterDepartment, @Nullable UserSummary assignee, SlaInfo sla,
		Instant createdAt, Instant updatedAt, @Nullable Instant firstResponseAt, @Nullable Instant resolvedAt,
		List<ApprovalStepView> approvals, List<CommentView> comments, List<AttachmentView> attachments,
		List<ActivityView> activity, Permissions permissions) {

	public record AttachmentView(long id, String fileName, String contentType, long sizeBytes, String uploadedBy,
			Instant createdAt) {
	}

	public record ApprovalStepView(long id, int order, String name, String approverName, ApprovalStatus status,
			@Nullable Instant pendingSince, @Nullable String decidedBy, @Nullable Instant decidedAt,
			@Nullable String comment) {
	}

	public record CommentView(long id, UserSummary author, String body, Instant createdAt) {
	}

	public record ActivityView(ActivityType type, String message, Instant at) {
	}

	public record Permissions(boolean canAssignToMe, List<TicketStatus> allowedStatuses, boolean canComment,
			boolean canDecideApproval) {
	}

}
