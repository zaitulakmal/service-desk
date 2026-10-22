package com.zaitul.servicedesk.approval;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.approval.ApprovalPolicy.PlannedStep;
import com.zaitul.servicedesk.ticket.TicketType;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.Roles;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalPolicyTests {

	private final ApprovalPolicy policy = new ApprovalPolicy();

	private final AppUser manager = new AppUser("farah@example.com", "Farah Aziz");

	private final AppUser requester = new AppUser("aina@example.com", "Aina Rahman");

	ApprovalPolicyTests() {
		this.requester.setManager(this.manager);
	}

	@Test
	void accessNeedsTheManagerThenItSecurity() {
		List<PlannedStep> steps = this.policy.stepsFor(TicketType.ACCESS, null, this.requester);

		assertThat(steps).extracting(PlannedStep::name).containsExactly("Manager approval", "IT Security approval");
		assertThat(steps.get(0).approver()).isSameAs(this.manager);
		assertThat(steps.get(1).approverRole()).isEqualTo(Roles.SECURITY_APPROVER);
	}

	@Test
	void purchasesNeedTheManagerOnlyAboveTheThreshold() {
		assertThat(this.policy.stepsFor(TicketType.PURCHASE, new BigDecimal("1000.00"), this.requester)).isEmpty();
		assertThat(this.policy.stepsFor(TicketType.PURCHASE, new BigDecimal("1000.01"), this.requester))
			.extracting(PlannedStep::name)
			.containsExactly("Manager approval");
	}

	@Test
	void incidentsAndServiceRequestsNeedNoApproval() {
		assertThat(this.policy.stepsFor(TicketType.INCIDENT, null, this.requester)).isEmpty();
		assertThat(this.policy.stepsFor(TicketType.SERVICE_REQUEST, null, this.requester)).isEmpty();
	}

	@Test
	void managerStepIsSkippedWhenNoManagerIsOnRecord() {
		AppUser noManager = new AppUser("hafiz@example.com", "Hafiz Osman");

		assertThat(this.policy.stepsFor(TicketType.ACCESS, null, noManager)).extracting(PlannedStep::name)
			.containsExactly("IT Security approval");
	}

}
