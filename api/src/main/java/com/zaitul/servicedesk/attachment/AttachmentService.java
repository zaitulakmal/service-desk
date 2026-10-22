package com.zaitul.servicedesk.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.approval.ApprovalStep;
import com.zaitul.servicedesk.approval.ApprovalStepRepository;
import com.zaitul.servicedesk.common.ConflictException;
import com.zaitul.servicedesk.common.InvalidFieldException;
import com.zaitul.servicedesk.common.NotFoundException;
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

	/** Screenshots, documents and logs. Anything else (e.g. executables) is refused. */
	static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp",
			"application/pdf", "text/plain", "text/csv",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation");

	private static final int MAX_FILE_NAME_LENGTH = 200;

	private final TicketRepository tickets;

	private final ApprovalStepRepository approvalSteps;

	private final TicketAttachmentRepository attachments;

	private final AppUserRepository users;

	private final AttachmentStorage storage;

	private final TicketAccess access;

	private final TicketDetailAssembler assembler;

	private final ActivityLog activityLog;

	private final Clock clock;

	AttachmentService(TicketRepository tickets, ApprovalStepRepository approvalSteps,
			TicketAttachmentRepository attachments, AppUserRepository users, AttachmentStorage storage,
			TicketAccess access, TicketDetailAssembler assembler, ActivityLog activityLog, Clock clock) {
		this.tickets = tickets;
		this.approvalSteps = approvalSteps;
		this.attachments = attachments;
		this.users = users;
		this.storage = storage;
		this.access = access;
		this.assembler = assembler;
		this.activityLog = activityLog;
		this.clock = clock;
	}

	/**
	 * Saves the row first, then the bytes. If the upload to storage fails, the exception
	 * rolls the row back, so the app never lists a file that does not exist.
	 */
	@Transactional
	public TicketDetailResponse upload(long ticketId, MultipartFile file, CurrentUser me) {
		Ticket ticket = this.tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
		List<ApprovalStep> chain = this.approvalSteps.findForTicket(ticketId);
		if (!this.access.canView(ticket, me, chain)) {
			throw new TicketNotFoundException(ticketId);
		}
		if (ticket.getStatus().isFinished()) {
			throw new ConflictException("Cannot add files to a ticket that is " + ticket.getStatus());
		}
		if (file.isEmpty()) {
			throw new InvalidFieldException("file", "is empty");
		}
		String contentType = normalise(file.getContentType());
		if (!ALLOWED_TYPES.contains(contentType)) {
			throw new InvalidFieldException("file", "Only images, PDFs, text and Office files can be attached");
		}
		String fileName = cleanFileName(file.getOriginalFilename());
		String key = "tickets/" + ticketId + "/" + UUID.randomUUID();
		AppUser uploader = this.users.findById(me.id()).orElseThrow();
		Instant now = this.clock.instant();
		this.attachments
			.save(new TicketAttachment(ticket, fileName, contentType, file.getSize(), key, uploader, now));
		try (InputStream content = file.getInputStream()) {
			this.storage.put(key, contentType, file.getSize(), content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		this.activityLog.record(ticket, uploader, ActivityType.ATTACHED, uploader.getFullName() + " attached " + fileName);
		return this.assembler.toDetail(ticket, me);
	}

	@Transactional(readOnly = true)
	public AttachmentDownload open(long ticketId, long attachmentId, CurrentUser me) {
		Ticket ticket = this.tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
		if (!this.access.canView(ticket, me, this.approvalSteps.findForTicket(ticketId))) {
			throw new TicketNotFoundException(ticketId);
		}
		TicketAttachment attachment = this.attachments.findByIdAndTicketId(attachmentId, ticketId)
			.orElseThrow(() -> new NotFoundException("Attachment " + attachmentId + " not found"));
		return new AttachmentDownload(attachment.getFileName(), attachment.getContentType(),
				attachment.getSizeBytes(), this.storage.open(attachment.getStorageKey()));
	}

	private static String normalise(@Nullable String contentType) {
		if (contentType == null) {
			return "";
		}
		int parameters = contentType.indexOf(';');
		return ((parameters >= 0) ? contentType.substring(0, parameters) : contentType).trim()
			.toLowerCase(Locale.ROOT);
	}

	/** Keeps only the last part of the name the browser sent and drops control characters. */
	static String cleanFileName(@Nullable String original) {
		if (original == null || original.isBlank()) {
			return "file";
		}
		String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1)
			.replaceAll("\\p{Cntrl}", "")
			.trim();
		if (name.isEmpty()) {
			return "file";
		}
		return (name.length() > MAX_FILE_NAME_LENGTH) ? name.substring(name.length() - MAX_FILE_NAME_LENGTH) : name;
	}

	public record AttachmentDownload(String fileName, String contentType, long sizeBytes, InputStream content) {
	}

}
