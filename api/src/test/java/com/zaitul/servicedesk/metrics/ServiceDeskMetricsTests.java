package com.zaitul.servicedesk.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDeskMetricsTests extends ApiTestSupport {

	@Autowired
	private MeterRegistry registry;

	@Test
	void countsTicketsAndApprovalsAndWatchesTheOutbox() throws Exception {
		double before = createdCount();

		long id = createAccessRequest(this.aina);
		decide(this.farah, id, "APPROVE", null);

		assertThat(createdCount()).isEqualTo(before + 1);
		assertThat(this.registry.get("servicedesk.approvals.decided").tag("decision", "approved").counter().count())
			.isGreaterThanOrEqualTo(1);
		// The relay is off in tests, so events stay pending.
		assertThat(this.registry.get("servicedesk.outbox.pending").gauge().value()).isGreaterThan(0);
	}

	private double createdCount() {
		var counter = this.registry.find("servicedesk.tickets.raised").tag("category", "IT").tag("type", "ACCESS")
			.counter();
		return (counter != null) ? counter.count() : 0;
	}

}
