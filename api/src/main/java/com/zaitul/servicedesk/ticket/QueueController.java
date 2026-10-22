package com.zaitul.servicedesk.ticket;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.common.PageResponse;
import com.zaitul.servicedesk.user.CurrentUser;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queue")
@PreAuthorize("hasRole('agent')")
class QueueController {

	private final TicketService ticketService;

	QueueController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping
	public PageResponse<TicketSummaryResponse> queue(@RequestParam(defaultValue = "ALL") QueueView view,
			@RequestParam(required = false) @Nullable TicketPriority priority,
			@RequestParam(name = "q", required = false) @Nullable String search,
			@PageableDefault(size = 20) Pageable pageable, CurrentUser me) {
		return this.ticketService.queue(me, view, priority, search, pageable);
	}

	@GetMapping("/counts")
	public Map<QueueView, Long> counts(CurrentUser me) {
		return this.ticketService.queueCounts(me);
	}

}
