package com.zaitul.servicedesk.ticket;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class SlaEscalationTests extends ApiTestSupport {

	@Autowired
	private SlaService slaService;

	@Test
	void overdueTicketsAreEscalatedToServiceManagersOnce() throws Exception {
		long overdue = createIncident(this.aina);
		createIncident(this.hafiz);
		this.jdbc.update("update ticket set sla_due_at = now() - interval '5 minutes' where id = ?", overdue);

		assertThat(this.slaService.escalateBreaches()).isEqualTo(1);
		assertThat(this.slaService.escalateBreaches()).isZero();

		this.mockMvc.perform(get("/api/tickets/{id}", overdue).with(as(this.aina)))
			.andExpect(jsonPath("$.sla.state").value("BREACHED"))
			.andExpect(jsonPath("$.sla.breachedAt").isNotEmpty())
			.andExpect(jsonPath("$.activity[0].type").value("SLA_BREACHED"));

		String payload = this.jdbc.queryForObject("select payload::text from outbox_event where type = 'SLA_BREACHED'",
				String.class);
		assertThat(payload).contains("siti@example.com");
	}

}
