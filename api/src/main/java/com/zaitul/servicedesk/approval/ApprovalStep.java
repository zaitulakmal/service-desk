package com.zaitul.servicedesk.approval;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.ticket.Ticket;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.CurrentUser;
import com.zaitul.servicedesk.user.Roles;

/**
 * One approval a request must pass. The approver is either a specific person (the
 * requester's manager) or anyone holding a role (e.g. any IT Security approver).
 */
@Entity
@Table(name = "approval_step")
public class ApprovalStep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id")
	private Ticket ticket;

	@Column(nullable = false)
	private int stepOrder;

	@Column(nullable = false, length = 100)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approver_id")
	private @Nullable AppUser approver;

	@Column(length = 50)
	private @Nullable String approverRole;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApprovalStatus status;

	private @Nullable Instant pendingSince;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "decided_by_id")
	private @Nullable AppUser decidedBy;

	private @Nullable Instant decidedAt;

	@Column(length = 1000)
	private @Nullable String comment;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected ApprovalStep() {
		// required by JPA
	}

	ApprovalStep(Ticket ticket, int stepOrder, ApprovalPolicy.PlannedStep planned, Instant now) {
		this.ticket = ticket;
		this.stepOrder = stepOrder;
		this.name = planned.name();
		this.approver = planned.approver();
		this.approverRole = planned.approverRole();
		this.status = ApprovalStatus.WAITING;
		if (stepOrder == 1) {
			activate(now);
		}
	}

	void activate(Instant now) {
		this.status = ApprovalStatus.PENDING;
		this.pendingSince = now;
	}

	public boolean canBeDecidedBy(CurrentUser user) {
		if (this.status != ApprovalStatus.PENDING) {
			return false;
		}
		return (this.approver != null) ? user.is(this.approver) : user.hasRole(this.approverRole);
	}

	/** True if this person may act on this step now or could once it is their turn. */
	public boolean involves(CurrentUser user) {
		return (this.approver != null) ? user.is(this.approver) : user.hasRole(this.approverRole);
	}

	void approve(AppUser by, @Nullable String comment, Instant now) {
		decide(ApprovalStatus.APPROVED, by, comment, now);
	}

	void reject(AppUser by, @Nullable String comment, Instant now) {
		decide(ApprovalStatus.REJECTED, by, comment, now);
	}

	private void decide(ApprovalStatus outcome, AppUser by, @Nullable String comment, Instant now) {
		this.status = outcome;
		this.decidedBy = by;
		this.decidedAt = now;
		this.comment = comment;
	}

	/** "Farah Aziz" for a personal approver, "IT Security" for a role. */
	public String getApproverName() {
		return (this.approver != null) ? this.approver.getFullName() : Roles.displayName(this.approverRole);
	}

	public Long getId() {
		return this.id;
	}

	public Ticket getTicket() {
		return this.ticket;
	}

	public int getStepOrder() {
		return this.stepOrder;
	}

	public String getName() {
		return this.name;
	}

	public @Nullable AppUser getApprover() {
		return this.approver;
	}

	public @Nullable String getApproverRole() {
		return this.approverRole;
	}

	public ApprovalStatus getStatus() {
		return this.status;
	}

	public @Nullable Instant getPendingSince() {
		return this.pendingSince;
	}

	public @Nullable AppUser getDecidedBy() {
		return this.decidedBy;
	}

	public @Nullable Instant getDecidedAt() {
		return this.decidedAt;
	}

	public @Nullable String getComment() {
		return this.comment;
	}

}
