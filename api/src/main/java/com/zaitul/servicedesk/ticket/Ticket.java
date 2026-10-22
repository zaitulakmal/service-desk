package com.zaitul.servicedesk.ticket;

import java.math.BigDecimal;
import java.time.Duration;
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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.common.ConflictException;
import com.zaitul.servicedesk.user.AppUser;

/**
 * A request for help. All rules about how a ticket may change live here, so they
 * hold no matter which endpoint or job changes it.
 */
@Entity
@Table(name = "ticket")
public class Ticket {

	/** Share of the SLA target left at which a ticket counts as "at risk". */
	private static final int AT_RISK_DIVISOR = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Optimistic locking: stops two agents silently overwriting each other's change. */
	@Version
	private long version;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketPriority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_id")
	private AppUser requester;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private @Nullable AppUser assignee;

	@Column(precision = 12, scale = 2)
	private @Nullable BigDecimal purchaseAmount;

	private long slaTargetSeconds;

	/** When the ticket must be resolved by. Empty while the SLA is paused. */
	private @Nullable Instant slaDueAt;

	/** Time left on the SLA, saved while it is paused. */
	private @Nullable Long slaRemainingSeconds;

	private @Nullable Instant slaBreachedAt;

	private @Nullable Instant firstResponseAt;

	private @Nullable Instant resolvedAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Ticket() {
		// required by JPA
	}

	/**
	 * Creates a ticket. If it needs approval it waits with the SLA paused; otherwise
	 * it is OPEN and the SLA clock starts now.
	 */
	public Ticket(String title, String description, TicketType type, TicketCategory category,
			TicketPriority priority, @Nullable BigDecimal purchaseAmount, AppUser requester, boolean needsApproval,
			Instant now) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.category = category;
		this.priority = priority;
		this.purchaseAmount = purchaseAmount;
		this.requester = requester;
		this.slaTargetSeconds = priority.getSlaTarget().toSeconds();
		if (needsApproval) {
			this.status = TicketStatus.AWAITING_APPROVAL;
			this.slaRemainingSeconds = this.slaTargetSeconds;
		}
		else {
			this.status = TicketStatus.OPEN;
			this.slaDueAt = now.plusSeconds(this.slaTargetSeconds);
		}
	}

	/** An agent takes the ticket. Taking an OPEN ticket starts work on it. */
	public void assignTo(AppUser agent, Instant now) {
		if (this.status.isFinished() || this.status == TicketStatus.RESOLVED) {
			throw new ConflictException("Cannot assign a ticket that is " + this.status);
		}
		this.assignee = agent;
		if (this.status == TicketStatus.OPEN) {
			this.status = TicketStatus.IN_PROGRESS;
		}
		recordFirstResponse(now);
	}

	public void changeStatus(TicketStatus newStatus, Instant now) {
		if (!this.status.canTransitionTo(newStatus)) {
			throw new InvalidStatusTransitionException(this.status, newStatus);
		}
		if (newStatus == TicketStatus.RESOLVED) {
			this.resolvedAt = now;
		}
		else if (this.status == TicketStatus.RESOLVED && newStatus == TicketStatus.IN_PROGRESS) {
			this.resolvedAt = null;
		}
		this.status = newStatus;
		recordFirstResponse(now);
	}

	/** Every approval passed: the team can start, and the paused SLA clock resumes. */
	public void approvalGranted(Instant now) {
		requireStatus(TicketStatus.AWAITING_APPROVAL);
		this.status = (this.assignee != null) ? TicketStatus.IN_PROGRESS : TicketStatus.OPEN;
		if (this.slaRemainingSeconds != null) {
			this.slaDueAt = now.plusSeconds(this.slaRemainingSeconds);
			this.slaRemainingSeconds = null;
		}
	}

	public void approvalRejected() {
		requireStatus(TicketStatus.AWAITING_APPROVAL);
		this.status = TicketStatus.REJECTED;
	}

	/** The first time the support team does something visible on the ticket. */
	public void recordFirstResponse(Instant now) {
		if (this.firstResponseAt == null) {
			this.firstResponseAt = now;
		}
	}

	/** Returns true the first time the ticket is found past its due time. */
	public boolean markSlaBreached(Instant now) {
		if (this.slaBreachedAt != null || slaState(now) != SlaState.BREACHED) {
			return false;
		}
		this.slaBreachedAt = now;
		return true;
	}

	public SlaState slaState(Instant now) {
		return switch (this.status) {
			case AWAITING_APPROVAL -> SlaState.PAUSED;
			case REJECTED -> SlaState.NONE;
			case RESOLVED, CLOSED -> {
				if (this.resolvedAt == null || this.slaDueAt == null) {
					yield SlaState.NONE;
				}
				yield this.resolvedAt.isAfter(this.slaDueAt) ? SlaState.MISSED : SlaState.MET;
			}
			case OPEN, IN_PROGRESS -> {
				long remaining = Duration.between(now, this.slaDueAt).toSeconds();
				if (remaining < 0) {
					yield SlaState.BREACHED;
				}
				yield (remaining * AT_RISK_DIVISOR < this.slaTargetSeconds) ? SlaState.AT_RISK : SlaState.ON_TRACK;
			}
		};
	}

	/** Seconds left on the SLA (negative once breached), or empty when no clock applies. */
	public @Nullable Long slaRemainingSeconds(Instant now) {
		if (this.status == TicketStatus.AWAITING_APPROVAL) {
			return this.slaRemainingSeconds;
		}
		if (TicketStatus.ACTIVE.contains(this.status) && this.slaDueAt != null) {
			return Duration.between(now, this.slaDueAt).toSeconds();
		}
		return null;
	}

	private void requireStatus(TicketStatus expected) {
		if (this.status != expected) {
			throw new ConflictException("Ticket is " + this.status + ", expected " + expected);
		}
	}

	/** Human-friendly reference shown to users, e.g. SD-0042. */
	public String getReference() {
		return "SD-%04d".formatted(this.id);
	}

	public Long getId() {
		return this.id;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDescription() {
		return this.description;
	}

	public TicketType getType() {
		return this.type;
	}

	public TicketCategory getCategory() {
		return this.category;
	}

	public TicketPriority getPriority() {
		return this.priority;
	}

	public TicketStatus getStatus() {
		return this.status;
	}

	public AppUser getRequester() {
		return this.requester;
	}

	public @Nullable AppUser getAssignee() {
		return this.assignee;
	}

	public @Nullable BigDecimal getPurchaseAmount() {
		return this.purchaseAmount;
	}

	public long getSlaTargetSeconds() {
		return this.slaTargetSeconds;
	}

	public @Nullable Instant getSlaDueAt() {
		return this.slaDueAt;
	}

	public @Nullable Instant getSlaBreachedAt() {
		return this.slaBreachedAt;
	}

	public @Nullable Instant getFirstResponseAt() {
		return this.firstResponseAt;
	}

	public @Nullable Instant getResolvedAt() {
		return this.resolvedAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
