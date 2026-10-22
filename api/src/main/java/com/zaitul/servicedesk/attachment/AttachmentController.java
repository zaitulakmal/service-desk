package com.zaitul.servicedesk.attachment;

import java.nio.charset.StandardCharsets;

import com.zaitul.servicedesk.attachment.AttachmentService.AttachmentDownload;
import com.zaitul.servicedesk.ticket.TicketDetailResponse;
import com.zaitul.servicedesk.user.CurrentUser;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
class AttachmentController {

	private final AttachmentService attachmentService;

	AttachmentController(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<TicketDetailResponse> upload(@PathVariable long ticketId,
			@RequestParam("file") MultipartFile file, CurrentUser me) {
		return ResponseEntity.status(HttpStatus.CREATED).body(this.attachmentService.upload(ticketId, file, me));
	}

	/**
	 * Streams the file through the API (rather than a link straight to storage), so the
	 * same access rules apply as for the ticket. Always sent as a download, never shown
	 * inline, so an uploaded HTML or SVG file cannot run in the browser.
	 */
	@GetMapping("/{attachmentId}")
	public ResponseEntity<InputStreamResource> download(@PathVariable long ticketId, @PathVariable long attachmentId,
			CurrentUser me) {
		AttachmentDownload download = this.attachmentService.open(ticketId, attachmentId, me);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
			.contentType(MediaType.parseMediaType(download.contentType()))
			.contentLength(download.sizeBytes())
			.body(new InputStreamResource(download.content()));
	}

}
