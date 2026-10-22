package com.zaitul.servicedesk.approval;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.ticket.TicketType;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.Roles;

import org.springframework.stereotype.Component;

/**
 * The company's approval rules, in one place:
 * <ul>
 * <li>Access to a system: the requester's manager, then IT Security.</li>
 * <li>Purchases above RM 1,000: the requester's manager.</li>
 * <li>Everything else: no approval.</li>
 * </ul>
 * If someone has no manager on record, the manager step is skipped.
 */
@Component
public class ApprovalPolicy {

	public static final BigDecimal PURCHASE_APPROVAL_THRESHOLD = new BigDecimal("1000.00");

	public List<PlannedStep> stepsFor(TicketType type, @Nullable BigDecimal purchaseAmount, AppUser requester) {
		boolean needsManager = type == TicketType.ACCESS || (type == TicketType.PURCHASE && purchaseAmount != null
				&& purchaseAmount.compareTo(PURCHASE_APPROVAL_THRESHOLD) > 0);
		List<PlannedStep> steps = new ArrayList<>();
		AppUser manager = requester.getManager();
		if (needsManager && manager != null) {
			steps.add(new PlannedStep("Manager approval", manager, null));
		}
		if (type == TicketType.ACCESS) {
			steps.add(new PlannedStep("IT Security approval", null, Roles.SECURITY_APPROVER));
		}
		return steps;
	}

	public record PlannedStep(String name, @Nullable AppUser approver, @Nullable String approverRole) {
	}

}
