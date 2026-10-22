package com.zaitul.servicedesk.ticket;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.zaitul.servicedesk.user.AppUser;

@Entity
@Table(name = "ticket_comment")
public class TicketComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id")
	private Ticket ticket;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id")
	private AppUser author;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Column(nullable = false)
	private Instant createdAt;

	protected TicketComment() {
		// required by JPA
	}

	public TicketComment(Ticket ticket, AppUser author, String body, Instant createdAt) {
		this.ticket = ticket;
		this.author = author;
		this.body = body;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return this.id;
	}

	public AppUser getAuthor() {
		return this.author;
	}

	public String getBody() {
		return this.body;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
