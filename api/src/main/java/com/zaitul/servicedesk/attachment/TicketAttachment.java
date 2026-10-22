package com.zaitul.servicedesk.attachment;

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

import com.zaitul.servicedesk.ticket.Ticket;
import com.zaitul.servicedesk.user.AppUser;

@Entity
@Table(name = "ticket_attachment")
public class TicketAttachment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id")
	private Ticket ticket;

	@Column(nullable = false, length = 255)
	private String fileName;

	@Column(nullable = false, length = 100)
	private String contentType;

	@Column(nullable = false)
	private long sizeBytes;

	/** Where the bytes are in the bucket. Random, so file names never collide or leak. */
	@Column(nullable = false, unique = true, length = 300)
	private String storageKey;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploaded_by_id")
	private AppUser uploadedBy;

	@Column(nullable = false)
	private Instant createdAt;

	protected TicketAttachment() {
		// required by JPA
	}

	TicketAttachment(Ticket ticket, String fileName, String contentType, long sizeBytes, String storageKey,
			AppUser uploadedBy, Instant createdAt) {
		this.ticket = ticket;
		this.fileName = fileName;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.storageKey = storageKey;
		this.uploadedBy = uploadedBy;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return this.id;
	}

	public String getFileName() {
		return this.fileName;
	}

	public String getContentType() {
		return this.contentType;
	}

	public long getSizeBytes() {
		return this.sizeBytes;
	}

	public String getStorageKey() {
		return this.storageKey;
	}

	public AppUser getUploadedBy() {
		return this.uploadedBy;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
