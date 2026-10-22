package com.zaitul.servicedesk.ticket;

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
import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.user.AppUser;

/** One line in a ticket's audit trail. Rows are only ever added, never changed. */
@Entity
@Table(name = "ticket_activity")
public class TicketActivity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id")
	private Ticket ticket;

	/** Who did it. Empty when the system did it, e.g. an SLA breach. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private @Nullable AppUser actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ActivityType type;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(nullable = false)
	private Instant createdAt;

	protected TicketActivity() {
		// required by JPA
	}

	TicketActivity(Ticket ticket, @Nullable AppUser actor, ActivityType type, String message, Instant createdAt) {
		this.ticket = ticket;
		this.actor = actor;
		this.type = type;
		this.message = message;
		this.createdAt = createdAt;
	}

	public Long getTicketId() {
		return this.ticket.getId();
	}

	public ActivityType getType() {
		return this.type;
	}

	public String getMessage() {
		return this.message;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
