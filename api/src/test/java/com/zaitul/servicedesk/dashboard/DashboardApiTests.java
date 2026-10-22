package com.zaitul.servicedesk.dashboard;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardApiTests extends ApiTestSupport {

	@Test
	void serviceManagersSeeTheOverview() throws Exception {
		long answered = createIncident(this.aina);
		long late = createIncident(this.hafiz);
		createAccessRequest(this.aina);
		this.mockMvc.perform(post("/api/tickets/{id}/assign", answered).with(as(this.daniel)));
		this.jdbc.update("update ticket set sla_due_at = now() - interval '1 minute' where id = ?", late);

		this.mockMvc.perform(get("/api/dashboard").param("days", "7").with(as(this.siti)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openRequests").value(2))
			.andExpect(jsonPath("$.awaitingApproval").value(1))
			.andExpect(jsonPath("$.averageFirstResponseSeconds").isNumber())
			.andExpect(jsonPath("$.openByCategory[0].category").value("IT"))
			.andExpect(jsonPath("$.openByCategory[0].count").value(2))
			.andExpect(jsonPath("$.needsAttention", hasSize(1)))
			.andExpect(jsonPath("$.needsAttention[0].id").value(late))
			.andExpect(jsonPath("$.approvalsWaitingLongest[0].waitingOn").value("Farah Aziz"));
	}

	@Test
	void otherPeopleCannotSeeIt() throws Exception {
		this.mockMvc.perform(get("/api/dashboard").with(as(this.daniel))).andExpect(status().isForbidden());
	}

}
