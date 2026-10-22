package com.zaitul.servicedesk.approval;

import java.util.List;

import jakarta.validation.Valid;

import com.zaitul.servicedesk.ticket.TicketDetailResponse;
import com.zaitul.servicedesk.user.CurrentUser;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ApprovalController {

	private final ApprovalService approvalService;

	ApprovalController(ApprovalService approvalService) {
		this.approvalService = approvalService;
	}

	/** Requests waiting for the signed-in user's decision. */
	@GetMapping("/api/approvals/pending")
	public List<PendingApprovalResponse> pending(CurrentUser me) {
		return this.approvalService.pendingFor(me);
	}

	@PostMapping("/api/tickets/{id}/approval")
	public TicketDetailResponse decide(@PathVariable long id, @Valid @RequestBody ApprovalDecisionRequest request,
			CurrentUser me) {
		return this.approvalService.decide(id, request, me);
	}

}
