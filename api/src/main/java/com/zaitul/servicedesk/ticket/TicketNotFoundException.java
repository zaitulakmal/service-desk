package com.zaitul.servicedesk.ticket;

import com.zaitul.servicedesk.common.NotFoundException;

public class TicketNotFoundException extends NotFoundException {

	public TicketNotFoundException(long id) {
		super("Ticket " + id + " not found");
	}

}
