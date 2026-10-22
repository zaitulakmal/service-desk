package com.zaitul.servicedesk.ticket;

import com.zaitul.servicedesk.common.ConflictException;

public class InvalidStatusTransitionException extends ConflictException {

	public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
		super("Cannot change status from " + from + " to " + to);
	}

}
