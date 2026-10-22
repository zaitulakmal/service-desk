package com.zaitul.servicedesk.ticket;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.zaitul.servicedesk.approval.ApprovalPolicy;

/** The choices on the "New request" form, so the web app never hard-codes business rules. */
public record TicketOptionsResponse(List<TypeOption> types, List<TicketCategory> categories,
		List<PriorityOption> priorities, BigDecimal purchaseApprovalThreshold) {

	static TicketOptionsResponse current() {
		return new TicketOptionsResponse(
				Arrays.stream(TicketType.values()).map((type) -> new TypeOption(type, type.getLabel())).toList(),
				List.of(TicketCategory.values()),
				Arrays.stream(TicketPriority.values())
					.map((priority) -> new PriorityOption(priority, priority.getSlaTarget().toSeconds()))
					.toList(),
				ApprovalPolicy.PURCHASE_APPROVAL_THRESHOLD);
	}

	public record TypeOption(TicketType value, String label) {
	}

	public record PriorityOption(TicketPriority value, long slaTargetSeconds) {
	}

}
