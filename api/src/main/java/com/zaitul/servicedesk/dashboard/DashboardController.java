package com.zaitul.servicedesk.dashboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
class DashboardController {

	private final DashboardService dashboardService;

	DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/api/dashboard")
	@PreAuthorize("hasRole('service-manager')")
	public DashboardResponse dashboard(@RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
		return this.dashboardService.summary(days);
	}

}
