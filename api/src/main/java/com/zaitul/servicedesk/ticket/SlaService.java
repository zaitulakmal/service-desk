package com.zaitul.servicedesk.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.zaitul.servicedesk.events.TicketEventType;
import com.zaitul.servicedesk.events.TicketEvents;
import com.zaitul.servicedesk.metrics.ServiceDeskMetrics;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.AppUserRepository;
import com.zaitul.servicedesk.user.Roles;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaService {

	private static final int BATCH_SIZE = 100;

	private final TicketRepository tickets;

	private final AppUserRepository users;

	private final ActivityLog activityLog;

	private final TicketEvents events;

	private final ServiceDeskMetrics metrics;

	private final Clock clock;

	SlaService(TicketRepository tickets, AppUserRepository users, ActivityLog activityLog, TicketEvents events,
			ServiceDeskMetrics metrics, Clock clock) {
		this.metrics = metrics;
		this.tickets = tickets;
		this.users = users;
		this.activityLog = activityLog;
		this.events = events;
		this.clock = clock;
	}

	/** Flags tickets that just went past their due time and escalates them to service managers. */
	@Transactional
	public int escalateBreaches() {
		Instant now = this.clock.instant();
		List<Ticket> overdue = this.tickets.lockOverdue(now, BATCH_SIZE);
		if (overdue.isEmpty()) {
			return 0;
		}
		List<AppUser> serviceManagers = this.users.findAllByRole(Roles.SERVICE_MANAGER);
		int breached = 0;
		for (Ticket ticket : overdue) {
			if (ticket.markSlaBreached(now)) {
				this.activityLog.record(ticket, null, ActivityType.SLA_BREACHED,
						"SLA breached. Escalated to service management.");
				this.events.publish(TicketEventType.SLA_BREACHED, ticket, null, serviceManagers, null);
				this.metrics.slaBreached(ticket);
				breached++;
			}
		}
		return breached;
	}

}
