package com.zaitul.servicedesk.approval;

public enum ApprovalStatus {

	/** A later step; its turn has not come yet. */
	WAITING,

	/** The step that needs a decision now. Only one step per ticket is pending. */
	PENDING,

	APPROVED,

	REJECTED

}
