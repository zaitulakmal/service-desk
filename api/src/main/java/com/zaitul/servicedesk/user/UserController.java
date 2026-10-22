package com.zaitul.servicedesk.user;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.ticket.TicketCategory;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class UserController {

	private final AppUserRepository users;

	UserController(AppUserRepository users) {
		this.users = users;
	}

	/** Who am I? The web app uses this to decide which menu items and screens to show. */
	@GetMapping("/api/me")
	@Transactional(readOnly = true)
	public ProfileResponse me(CurrentUser me) {
		AppUser user = this.users.findById(me.id()).orElseThrow();
		AppUser manager = user.getManager();
		return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getDepartment(),
				user.getJobTitle(), user.getTeam(), me.roles(), (manager != null) ? manager.getFullName() : null);
	}

	public record ProfileResponse(UUID id, String fullName, String email, @Nullable String department,
			@Nullable String jobTitle, @Nullable TicketCategory team, Set<String> roles,
			@Nullable String managerName) {
	}

}
