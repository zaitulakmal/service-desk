package com.zaitul.servicedesk.approval;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApprovalApiTests extends ApiTestSupport {

	@Test
	void accessRequestWaitsForTheManagerThenItSecurity() throws Exception {
		long id = createAccessRequest(this.aina);

		this.mockMvc.perform(get("/api/tickets/{id}", id).with(as(this.aina)))
			.andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
			.andExpect(jsonPath("$.sla.state").value("PAUSED"))
			.andExpect(jsonPath("$.approvals[0].approverName").value("Farah Aziz"))
			.andExpect(jsonPath("$.approvals[0].status").value("PENDING"))
			.andExpect(jsonPath("$.approvals[1].approverName").value("IT Security"))
			.andExpect(jsonPath("$.approvals[1].status").value("WAITING"));

		// Only the person whose turn it is sees it in their list, and only they can decide.
		this.mockMvc.perform(get("/api/approvals/pending").with(as(this.farah))).andExpect(jsonPath("$", hasSize(1)));
		this.mockMvc.perform(get("/api/approvals/pending").with(as(this.daniel))).andExpect(jsonPath("$", hasSize(0)));
		decide(this.daniel, id, "APPROVE", null).andExpect(status().isForbidden());

		decide(this.farah, id, "APPROVE", "Read-only is enough").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
			.andExpect(jsonPath("$.approvals[0].status").value("APPROVED"))
			.andExpect(jsonPath("$.approvals[0].decidedBy").value("Farah Aziz"))
			.andExpect(jsonPath("$.approvals[1].status").value("PENDING"));

		this.mockMvc.perform(get("/api/approvals/pending").with(as(this.daniel))).andExpect(jsonPath("$", hasSize(1)));

		decide(this.daniel, id, "APPROVE", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.sla.state").value("ON_TRACK"))
			.andExpect(jsonPath("$.sla.dueAt").isNotEmpty())
			.andExpect(jsonPath("$.activity[0].message").value("All approvals done. SLA resumed."));

		assertThat(outboxCount("APPROVAL_REQUESTED")).isEqualTo(2);
		assertThat(outboxCount("TICKET_APPROVED")).isEqualTo(1);
	}

	@Test
	void rejectingNeedsAReasonAndEndsTheRequest() throws Exception {
		long id = createAccessRequest(this.aina);

		decide(this.farah, id, "REJECT", null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.comment").exists());
		decide(this.farah, id, "REJECT", "Use the shared report instead").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.sla.state").value("NONE"))
			.andExpect(jsonPath("$.approvals[0].comment").value("Use the shared report instead"));

		decide(this.daniel, id, "APPROVE", null).andExpect(status().isConflict());
		assertThat(outboxCount("TICKET_REJECTED")).isEqualTo(1);
	}

	@Test
	void nobodyApprovesTheirOwnRequest() throws Exception {
		// Daniel is an IT Security approver, so his own access request reaches him at step 2.
		long id = createAccessRequest(this.daniel);
		decide(this.siti, id, "APPROVE", null).andExpect(status().isOk());

		decide(this.daniel, id, "APPROVE", null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("You cannot approve your own request"));
	}

	@Test
	void smallPurchasesSkipApproval() throws Exception {
		long small = createTicket(this.aina, """
				{ "title": "Keyboard", "description": "Mine is broken", "type": "PURCHASE",
				  "category": "IT", "priority": "LOW", "purchaseAmount": 250.00 }
				""");
		long large = createTicket(this.aina, """
				{ "title": "Monitor", "description": "27 inch", "type": "PURCHASE",
				  "category": "IT", "priority": "LOW", "purchaseAmount": 1200.00 }
				""");

		this.mockMvc.perform(get("/api/tickets/{id}", small).with(as(this.aina)))
			.andExpect(jsonPath("$.status").value("OPEN"));
		this.mockMvc.perform(get("/api/tickets/{id}", large).with(as(this.aina)))
			.andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
			.andExpect(jsonPath("$.approvals", hasSize(1)));
	}

	@Test
	void approversCanOpenTheTicketTheyAreAskedAbout() throws Exception {
		long id = createAccessRequest(this.aina);

		this.mockMvc.perform(get("/api/tickets/{id}", id).with(as(this.farah)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.permissions.canDecideApproval").value(true));
	}

}
