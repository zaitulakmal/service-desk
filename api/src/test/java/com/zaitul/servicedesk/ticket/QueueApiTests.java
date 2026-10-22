package com.zaitul.servicedesk.ticket;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QueueApiTests extends ApiTestSupport {

	@Test
	void agentsSeeTheirTeamsOpenWorkByTab() throws Exception {
		long taken = createIncident(this.aina);
		createIncident(this.hafiz);
		createAccessRequest(this.aina);
		this.mockMvc.perform(post("/api/tickets/{id}/assign", taken).with(as(this.daniel)));

		this.mockMvc.perform(get("/api/queue/counts").with(as(this.daniel)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ALL").value(3))
			.andExpect(jsonPath("$.MINE").value(1))
			.andExpect(jsonPath("$.UNASSIGNED").value(2));

		this.mockMvc.perform(get("/api/queue").param("view", "MINE").with(as(this.daniel)))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(taken));
	}

	@Test
	void mostUrgentSlaComesFirstAndPausedTicketsLast() throws Exception {
		long access = createAccessRequest(this.aina);
		long high = createIncident(this.aina);
		long urgent = createTicket(this.hafiz, """
				{ "title": "VPN not connecting", "description": "Times out", "type": "INCIDENT",
				  "category": "IT", "priority": "URGENT" }
				""");

		this.mockMvc.perform(get("/api/queue").with(as(this.daniel)))
			.andExpect(jsonPath("$.content[0].id").value(urgent))
			.andExpect(jsonPath("$.content[1].id").value(high))
			.andExpect(jsonPath("$.content[2].id").value(access))
			.andExpect(jsonPath("$.content[2].sla.state").value("PAUSED"));
	}

	@Test
	void searchMatchesTitleOrTicketNumber() throws Exception {
		long id = createIncident(this.aina);
		createAccessRequest(this.aina);

		this.mockMvc.perform(get("/api/queue").param("q", "flicker").with(as(this.daniel)))
			.andExpect(jsonPath("$.totalElements").value(1));
		this.mockMvc.perform(get("/api/queue").param("q", "SD-%04d".formatted(id)).with(as(this.daniel)))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(id));
	}

	@Test
	void agentsOnlySeeTheirOwnTeam() throws Exception {
		createIncident(this.aina);

		this.mockMvc.perform(get("/api/queue").with(as(this.mei))).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void staffCannotOpenTheQueue() throws Exception {
		this.mockMvc.perform(get("/api/queue").with(as(this.aina))).andExpect(status().isForbidden());
	}

}
