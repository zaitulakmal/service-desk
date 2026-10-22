package com.zaitul.servicedesk.ticket;

import java.net.URI;
import java.util.Map;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.common.PageResponse;
import com.zaitul.servicedesk.user.CurrentUser;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/tickets")
class TicketController {

	private final TicketService ticketService;

	TicketController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping("/options")
	public TicketOptionsResponse options() {
		return TicketOptionsResponse.current();
	}

	@PostMapping
	public ResponseEntity<TicketDetailResponse> create(@Valid @RequestBody CreateTicketRequest request,
			CurrentUser me) {
		TicketDetailResponse ticket = this.ticketService.create(request, me);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(ticket.id())
			.toUri();
		return ResponseEntity.created(location).body(ticket);
	}

	/** "My requests": tickets the signed-in user raised. */
	@GetMapping("/mine")
	public PageResponse<TicketSummaryResponse> mine(@RequestParam(required = false) @Nullable TicketStatus status,
			@PageableDefault(size = 20) Pageable pageable, CurrentUser me) {
		return this.ticketService.myRequests(me, status, pageable);
	}

	@GetMapping("/mine/counts")
	public Map<TicketStatus, Long> myCounts(CurrentUser me) {
		return this.ticketService.myRequestCounts(me);
	}

	@GetMapping("/{id}")
	public TicketDetailResponse get(@PathVariable long id, CurrentUser me) {
		return this.ticketService.detail(id, me);
	}

	@PostMapping("/{id}/assign")
	public TicketDetailResponse assignToMe(@PathVariable long id, CurrentUser me) {
		return this.ticketService.assignToMe(id, me);
	}

	@PatchMapping("/{id}/status")
	public TicketDetailResponse changeStatus(@PathVariable long id, @Valid @RequestBody ChangeStatusRequest request,
			CurrentUser me) {
		return this.ticketService.changeStatus(id, request.status(), me);
	}

	@PostMapping("/{id}/comments")
	public ResponseEntity<TicketDetailResponse> addComment(@PathVariable long id,
			@Valid @RequestBody AddCommentRequest request, CurrentUser me) {
		return ResponseEntity.status(HttpStatus.CREATED).body(this.ticketService.addComment(id, request.body(), me));
	}

}
