package com.zaitul.servicedesk.ticket;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.approval.ApprovalPolicy;
import com.zaitul.servicedesk.approval.ApprovalStep;
import com.zaitul.servicedesk.approval.ApprovalStepRepository;
import com.zaitul.servicedesk.approval.ApprovalWorkflow;
import com.zaitul.servicedesk.common.ConflictException;
import com.zaitul.servicedesk.common.InvalidFieldException;
import com.zaitul.servicedesk.common.PageResponse;
import com.zaitul.servicedesk.events.TicketEventType;
import com.zaitul.servicedesk.events.TicketEvents;
import com.zaitul.servicedesk.metrics.ServiceDeskMetrics;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.AppUserRepository;
import com.zaitul.servicedesk.user.CurrentUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

	/** Tickets that still belong in an agent's queue. */
	private static final List<TicketStatus> QUEUE_STATUSES = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS,
			TicketStatus.AWAITING_APPROVAL);

	private final TicketRepository tickets;

	private final AppUserRepository users;

	private final ApprovalPolicy approvalPolicy;

	private final ApprovalWorkflow approvalWorkflow;

	private final ApprovalStepRepository approvalSteps;

	private final TicketCommentRepository comments;

	private final TicketActivityRepository activities;

	private final ActivityLog activityLog;

	private final TicketEvents events;

	private final TicketAccess access;

	private final TicketDetailAssembler assembler;

	private final ServiceDeskMetrics metrics;

	private final Clock clock;

	TicketService(TicketRepository tickets, AppUserRepository users, ApprovalPolicy approvalPolicy,
			ApprovalWorkflow approvalWorkflow, ApprovalStepRepository approvalSteps, TicketCommentRepository comments,
			TicketActivityRepository activities, ActivityLog activityLog, TicketEvents events, TicketAccess access,
			TicketDetailAssembler assembler, ServiceDeskMetrics metrics, Clock clock) {
		this.metrics = metrics;
		this.tickets = tickets;
		this.users = users;
		this.approvalPolicy = approvalPolicy;
		this.approvalWorkflow = approvalWorkflow;
		this.approvalSteps = approvalSteps;
		this.comments = comments;
		this.activities = activities;
		this.activityLog = activityLog;
		this.events = events;
		this.access = access;
		this.assembler = assembler;
		this.clock = clock;
	}

	@Transactional
	public TicketDetailResponse create(CreateTicketRequest request, CurrentUser me) {
		BigDecimal purchaseAmount = null;
		if (request.type() == TicketType.PURCHASE) {
			if (request.purchaseAmount() == null) {
				throw new InvalidFieldException("purchaseAmount", "is required for purchases");
			}
			purchaseAmount = request.purchaseAmount();
		}
		AppUser requester = this.users.findById(me.id()).orElseThrow();
		List<ApprovalPolicy.PlannedStep> approvals = this.approvalPolicy.stepsFor(request.type(), purchaseAmount,
				requester);
		Instant now = this.clock.instant();
		Ticket ticket = this.tickets.save(new Ticket(request.title(), request.description(), request.type(),
				request.category(), request.priority(), purchaseAmount, requester, !approvals.isEmpty(), now));
		this.activityLog.record(ticket, requester, ActivityType.CREATED,
				requester.getFullName() + " created the request");
		this.events.publish(TicketEventType.TICKET_CREATED, ticket, requester);
		this.metrics.ticketCreated(ticket);
		if (!approvals.isEmpty()) {
			this.approvalWorkflow.start(ticket, approvals, now);
		}
		return this.assembler.toDetail(ticket, me);
	}

	@Transactional(readOnly = true)
	public PageResponse<TicketSummaryResponse> myRequests(CurrentUser me, @Nullable TicketStatus status,
			Pageable pageable) {
		List<Specification<Ticket>> filters = new ArrayList<>();
		filters.add(TicketSpecifications.requestedBy(me.id()));
		if (status != null) {
			filters.add(TicketSpecifications.hasStatus(status));
		}
		Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by(Sort.Direction.DESC, "updatedAt"));
		return summaries(this.tickets.findAll(Specification.allOf(filters), newestFirst));
	}

	@Transactional(readOnly = true)
	public Map<TicketStatus, Long> myRequestCounts(CurrentUser me) {
		Map<TicketStatus, Long> counts = new EnumMap<>(TicketStatus.class);
		for (TicketStatus status : TicketStatus.values()) {
			counts.put(status, 0L);
		}
		this.tickets.countByStatusForRequester(me.id())
			.forEach((count) -> counts.put(count.getStatus(), count.getTotal()));
		return counts;
	}

	/** The agent's queue: their team's open work, most urgent SLA first. */
	@Transactional(readOnly = true)
	public PageResponse<TicketSummaryResponse> queue(CurrentUser me, QueueView view,
			@Nullable TicketPriority priority, @Nullable String search, Pageable pageable) {
		List<Specification<Ticket>> filters = queueFilters(me, view);
		if (priority != null) {
			filters.add(TicketSpecifications.hasPriority(priority));
		}
		if (search != null && !search.isBlank()) {
			filters.add(TicketSpecifications.matches(search));
		}
		// Paused tickets have no due time; Postgres sorts those last.
		Pageable mostUrgentFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by(Sort.Order.asc("slaDueAt"), Sort.Order.asc("createdAt")));
		return summaries(this.tickets.findAll(Specification.allOf(filters), mostUrgentFirst));
	}

	@Transactional(readOnly = true)
	public Map<QueueView, Long> queueCounts(CurrentUser me) {
		Map<QueueView, Long> counts = new EnumMap<>(QueueView.class);
		for (QueueView view : QueueView.values()) {
			counts.put(view, this.tickets.count(Specification.allOf(queueFilters(me, view))));
		}
		return counts;
	}

	@Transactional(readOnly = true)
	public TicketDetailResponse detail(long id, CurrentUser me) {
		Ticket ticket = findVisible(id, me);
		return this.assembler.toDetail(ticket, me);
	}

	@Transactional
	public TicketDetailResponse assignToMe(long id, CurrentUser me) {
		Ticket ticket = findVisible(id, me);
		requireCanWork(ticket, me);
		AppUser agent = this.users.findById(me.id()).orElseThrow();
		ticket.assignTo(agent, this.clock.instant());
		this.activityLog.record(ticket, agent, ActivityType.ASSIGNED, agent.getFullName() + " took the request");
		this.events.publish(TicketEventType.TICKET_ASSIGNED, ticket, agent);
		return this.assembler.toDetail(ticket, me);
	}

	@Transactional
	public TicketDetailResponse changeStatus(long id, TicketStatus newStatus, CurrentUser me) {
		Ticket ticket = findVisible(id, me);
		requireCanWork(ticket, me);
		TicketStatus oldStatus = ticket.getStatus();
		ticket.changeStatus(newStatus, this.clock.instant());
		AppUser agent = this.users.findById(me.id()).orElseThrow();
		String change = label(oldStatus) + " → " + label(newStatus);
		this.activityLog.record(ticket, agent, ActivityType.STATUS_CHANGED,
				agent.getFullName() + " changed the status: " + change);
		this.events.publish(TicketEventType.STATUS_CHANGED, ticket, agent, List.of(), change);
		return this.assembler.toDetail(ticket, me);
	}

	@Transactional
	public TicketDetailResponse addComment(long id, String body, CurrentUser me) {
		Ticket ticket = this.tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
		List<ApprovalStep> chain = this.approvalSteps.findForTicket(id);
		if (!this.access.canView(ticket, me, chain)) {
			throw new TicketNotFoundException(id);
		}
		if (ticket.getStatus().isFinished()) {
			throw new ConflictException("Cannot comment on a ticket that is " + ticket.getStatus());
		}
		AppUser author = this.users.findById(me.id()).orElseThrow();
		Instant now = this.clock.instant();
		this.comments.save(new TicketComment(ticket, author, body, now));
		if (this.access.canWork(ticket, me)) {
			ticket.recordFirstResponse(now);
		}
		this.activityLog.record(ticket, author, ActivityType.COMMENTED, author.getFullName() + " replied");
		this.events.publish(TicketEventType.COMMENT_ADDED, ticket, author, List.of(), body);
		return this.assembler.toDetail(ticket, me, chain);
	}

	private List<Specification<Ticket>> queueFilters(CurrentUser me, QueueView view) {
		TicketCategory team = me.team();
		if (team == null) {
			throw new AccessDeniedException("You are not in a support team");
		}
		List<Specification<Ticket>> filters = new ArrayList<>();
		filters.add(TicketSpecifications.inCategory(team));
		filters.add(TicketSpecifications.statusIn(QUEUE_STATUSES));
		switch (view) {
			case UNASSIGNED -> filters.add(TicketSpecifications.unassigned());
			case MINE -> filters.add(TicketSpecifications.assignedTo(me.id()));
			case ALL -> {
			}
		}
		return filters;
	}

	private Ticket findVisible(long id, CurrentUser me) {
		Ticket ticket = this.tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
		if (!this.access.canView(ticket, me, this.approvalSteps.findForTicket(id))) {
			throw new TicketNotFoundException(id);
		}
		return ticket;
	}

	private void requireCanWork(Ticket ticket, CurrentUser me) {
		if (!this.access.canWork(ticket, me)) {
			throw new AccessDeniedException("Only " + ticket.getCategory() + " agents can work on this ticket");
		}
	}

	private PageResponse<TicketSummaryResponse> summaries(Page<Ticket> page) {
		List<Long> ids = page.getContent().stream().map(Ticket::getId).toList();
		Map<Long, TicketActivity> latest = ids.isEmpty() ? Map.of()
				: this.activities.findLatestForTickets(ids)
					.stream()
					.collect(Collectors.toMap(TicketActivity::getTicketId, Function.identity()));
		Instant now = this.clock.instant();
		return PageResponse.from(page.map((ticket) -> TicketSummaryResponse.of(ticket, latest.get(ticket.getId()), now)));
	}

	private static String label(TicketStatus status) {
		String words = status.name().replace('_', ' ').toLowerCase(Locale.ROOT);
		return Character.toUpperCase(words.charAt(0)) + words.substring(1);
	}

}
