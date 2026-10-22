package com.zaitul.servicedesk.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "servicedesk.sla.breach-check.enabled", havingValue = "true", matchIfMissing = true)
class SlaBreachJob {

	private static final Logger logger = LoggerFactory.getLogger(SlaBreachJob.class);

	private final SlaService slaService;

	SlaBreachJob(SlaService slaService) {
		this.slaService = slaService;
	}

	@Scheduled(fixedDelayString = "${servicedesk.sla.breach-check.interval:PT1M}")
	void run() {
		int breached = this.slaService.escalateBreaches();
		if (breached > 0) {
			logger.info("Escalated {} ticket(s) that breached their SLA", breached);
		}
	}

}
