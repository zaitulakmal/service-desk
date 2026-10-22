package com.zaitul.servicedesk.ticket;

public enum SlaState {

	/** Plenty of time left. */
	ON_TRACK,

	/** Less than 20% of the target time left. */
	AT_RISK,

	/** Past the due time and still not resolved. */
	BREACHED,

	/** Clock stopped while the request waits for approval. */
	PAUSED,

	/** Resolved in time. */
	MET,

	/** Resolved, but late. */
	MISSED,

	/** No SLA applies, e.g. the request was rejected or cancelled. */
	NONE

}
