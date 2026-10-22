package com.zaitul.servicedesk.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zaitul.servicedesk.approval.ApprovalStepRepository;
import com.zaitul.servicedesk.dashboard.DashboardResponse.CategoryCount;
import com.zaitul.servicedesk.dashboard.DashboardResponse.WaitingApproval;
import com.zaitul.servicedesk.ticket.SlaState;
import com.zaitul.servicedesk.ticket.TicketCategory;
import com.zaitul.servicedesk.ticket.TicketRepository;
import com.zaitul.servicedesk.ticket.TicketStatus;
import com.zaitul.servicedesk.ticket.TicketSummaryResponse;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private static final Set<SlaState> NEEDS_ATTENTION = Set.of(SlaState.BREACHED, SlaState.AT_RISK);

	private static final int LIST_SIZE = 5;

	private final TicketRepository tickets;

	private final ApprovalStepRepository approvalSteps;

	private final Clock clock;

	DashboardService(TicketRepository tickets, ApprovalStepRepository approvalSteps, Clock clock) {
		this.tickets = tickets;
		this.approvalSteps = approvalSteps;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardResponse summary(int days) {
		Instant now = this.clock.instant();
		Instant since = now.minus(Duration.ofDays(days));

		Map<TicketCategory, Long> byCategory = new EnumMap<>(TicketCategory.class);
		Arrays.stream(TicketCategory.values()).forEach((category) -> byCategory.put(category, 0L));
		this.tickets.countByCategory(TicketStatus.ACTIVE)
			.forEach((count) -> byCategory.put(count.getCategory(), count.getTotal()));

		List<TicketSummaryResponse> needsAttention = this.tickets.findBySlaDue(TicketStatus.ACTIVE, Limit.of(50))
			.stream()
			.filter((ticket) -> NEEDS_ATTENTION.contains(ticket.slaState(now)))
			.limit(LIST_SIZE)
			.map((ticket) -> TicketSummaryResponse.of(ticket, null, now))
			.toList();

		List<WaitingApproval> waiting = this.approvalSteps.findOldestPending(Limit.of(LIST_SIZE))
			.stream()
			.map((step) -> new WaitingApproval(step.getTicket().getId(), step.getTicket().getReference(),
					step.getTicket().getTitle(), step.getApproverName(), step.getPendingSince()))
			.toList();

		Double averageFirstResponse = this.tickets.averageFirstResponseSeconds(since);
		return new DashboardResponse(days, this.tickets.countByStatusIn(TicketStatus.ACTIVE),
				this.tickets.countByStatus(TicketStatus.AWAITING_APPROVAL),
				this.tickets.countBySlaBreachedAtAfter(since),
				(averageFirstResponse != null) ? Math.round(averageFirstResponse) : null,
				byCategory.entrySet().stream().map((e) -> new CategoryCount(e.getKey(), e.getValue())).toList(),
				needsAttention, waiting);
	}

}
