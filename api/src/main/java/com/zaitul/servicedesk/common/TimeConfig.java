package com.zaitul.servicedesk.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class TimeConfig {

	/** Code asks this clock for "now" instead of calling Instant.now(), so tests can control time. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
