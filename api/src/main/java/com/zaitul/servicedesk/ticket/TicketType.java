package com.zaitul.servicedesk.ticket;

/** What kind of help is needed. The type decides whether approvals are required. */
public enum TicketType {

	/** Something is broken. */
	INCIDENT("Something is broken"),

	/** Anything else that does not need approval, e.g. "update my bank details". */
	SERVICE_REQUEST("Ask for something"),

	/** Access to a system. Needs the manager and IT Security to approve. */
	ACCESS("Access to a system"),

	/** Buying something. Needs the manager's approval above the purchase threshold. */
	PURCHASE("Buy something");

	private final String label;

	TicketType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return this.label;
	}

}
