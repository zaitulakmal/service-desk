package com.zaitul.servicedesk.ticket;

import java.time.Clock;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.user.AppUser;

import org.springframework.stereotype.Component;

/** Writes the audit trail. Call it inside the same transaction as the change it records. */
@Component
public class ActivityLog {

	private final TicketActivityRepository activities;

	private final Clock clock;

	ActivityLog(TicketActivityRepository activities, Clock clock) {
		this.activities = activities;
		this.clock = clock;
	}

	public void record(Ticket ticket, @Nullable AppUser actor, ActivityType type, String message) {
		this.activities.save(new TicketActivity(ticket, actor, type, message, this.clock.instant()));
	}

}
