package com.zaitul.servicedesk.ticket;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

class TicketApiTests extends ApiTestSupport {

	@Test
	void requestsWithoutATokenAreRejected() throws Exception {
		this.mockMvc.perform(get("/api/tickets/mine")).andExpect(status().isUnauthorized());
	}

	@Test
	void staffRaiseATicketAndTheSlaClockStarts() throws Exception {
		this.mockMvc
			.perform(post("/api/tickets").with(as(this.aina)).contentType(MediaType.APPLICATION_JSON).content("""
					{ "title": "Laptop screen flickering when docked",
					  "description": "Flickers every few seconds when connected to the dock.",
					  "type": "INCIDENT", "category": "IT", "priority": "HIGH" }
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern(".*/api/tickets/\\d+")))
			.andExpect(jsonPath("$.reference").value(matchesPattern("SD-\\d{4,}")))
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.requester.fullName").value("Aina Rahman"))
			.andExpect(jsonPath("$.sla.state").value("ON_TRACK"))
			.andExpect(jsonPath("$.sla.targetSeconds").value(8 * 3600))
			.andExpect(jsonPath("$.activity[0].message").value("Aina Rahman created the request"))
			.andExpect(jsonPath("$.permissions.canComment").value(true))
			.andExpect(jsonPath("$.permissions.allowedStatuses", hasSize(0)));

		assertThat(outboxCount("TICKET_CREATED")).isEqualTo(1);
	}

	@Test
	void rejectsInvalidTicketWithAnErrorPerField() throws Exception {
		this.mockMvc
			.perform(post("/api/tickets").with(as(this.aina)).contentType(MediaType.APPLICATION_JSON).content("""
					{ "title": "", "description": "Help", "type": "INCIDENT", "priority": "HIGH" }
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.title").exists())
			.andExpect(jsonPath("$.errors.category").exists())
			.andExpect(jsonPath("$.errors.description").doesNotExist());
	}

	@Test
	void purchasesMustSayHowMuch() throws Exception {
		this.mockMvc
			.perform(post("/api/tickets").with(as(this.aina)).contentType(MediaType.APPLICATION_JSON).content("""
					{ "title": "Monitor", "description": "27 inch", "type": "PURCHASE",
					  "category": "IT", "priority": "LOW" }
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.purchaseAmount").value("is required for purchases"));
	}

	@Test
	void peopleOutsideTheTicketCannotSeeIt() throws Exception {
		long id = createIncident(this.aina);

		this.mockMvc.perform(get("/api/tickets/{id}", id).with(as(this.hafiz))).andExpect(status().isNotFound());
		this.mockMvc.perform(get("/api/tickets/{id}", id).with(as(this.mei))).andExpect(status().isNotFound());
		this.mockMvc.perform(get("/api/tickets/{id}", id).with(as(this.daniel))).andExpect(status().isOk());
		this.mockMvc.perform(get("/api/tickets/{id}", id).with(as(this.siti))).andExpect(status().isOk());
	}

	@Test
	void agentTakesTheTicketAndWorksItToClosed() throws Exception {
		long id = createIncident(this.aina);

		this.mockMvc.perform(post("/api/tickets/{id}/assign", id).with(as(this.daniel)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.assignee.fullName").value("Daniel Lee"))
			.andExpect(jsonPath("$.firstResponseAt").isNotEmpty())
			.andExpect(jsonPath("$.permissions.allowedStatuses[0]").value("RESOLVED"));

		changeStatus(this.daniel, id, "RESOLVED").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("RESOLVED"))
			.andExpect(jsonPath("$.sla.state").value("MET"));
		changeStatus(this.daniel, id, "CLOSED").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CLOSED"))
			.andExpect(jsonPath("$.permissions.canComment").value(false));

		assertThat(outboxCount("TICKET_ASSIGNED")).isEqualTo(1);
		assertThat(outboxCount("STATUS_CHANGED")).isEqualTo(2);
	}

	@Test
	void skippingStepsIsAConflict() throws Exception {
		long id = createIncident(this.aina);

		changeStatus(this.daniel, id, "RESOLVED").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Cannot change status from OPEN to RESOLVED"));
	}

	@Test
	void onlyAgentsOfTheTicketsTeamCanWorkIt() throws Exception {
		long id = createIncident(this.aina);

		changeStatus(this.aina, id, "IN_PROGRESS").andExpect(status().isForbidden());
		this.mockMvc.perform(post("/api/tickets/{id}/assign", id).with(as(this.siti)))
			.andExpect(status().isForbidden());
	}

	@Test
	void requesterAndAgentTalkInComments() throws Exception {
		long id = createIncident(this.aina);

		this.mockMvc
			.perform(post("/api/tickets/{id}/comments", id).with(as(this.daniel))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"body\": \"Could you try another cable?\" }"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.firstResponseAt").isNotEmpty());
		this.mockMvc
			.perform(post("/api/tickets/{id}/comments", id).with(as(this.aina))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"body\": \"Tried it, still flickers.\" }"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.comments", hasSize(2)))
			.andExpect(jsonPath("$.comments[1].author.fullName").value("Aina Rahman"))
			.andExpect(jsonPath("$.activity[0].message").value("Aina Rahman replied"));
	}

	@Test
	void myRequestsListsOnlyMyTicketsWithTheirLatestUpdate() throws Exception {
		long mine = createIncident(this.aina);
		createIncident(this.hafiz);
		this.mockMvc.perform(post("/api/tickets/{id}/assign", mine).with(as(this.daniel)));

		this.mockMvc.perform(get("/api/tickets/mine").with(as(this.aina)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].latestUpdate.message").value("Daniel Lee took the request"));

		this.mockMvc.perform(get("/api/tickets/mine/counts").with(as(this.aina)))
			.andExpect(jsonPath("$.IN_PROGRESS").value(1))
			.andExpect(jsonPath("$.OPEN").value(0));
	}

	@Test
	void optionsDescribeTheFormChoices() throws Exception {
		this.mockMvc.perform(get("/api/tickets/options").with(as(this.aina)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.types", hasSize(4)))
			.andExpect(jsonPath("$.priorities[3].value").value("URGENT"))
			.andExpect(jsonPath("$.priorities[3].slaTargetSeconds").value(4 * 3600))
			.andExpect(jsonPath("$.purchaseApprovalThreshold").value(1000.00));
	}

}
