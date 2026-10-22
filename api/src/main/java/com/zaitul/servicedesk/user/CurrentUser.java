package com.zaitul.servicedesk.user;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.ticket.TicketCategory;

/**
 * The signed-in user for the current request. Add a {@code CurrentUser} parameter to a
 * controller method and Spring fills it in (see {@link CurrentUserArgumentResolver}).
 */
public record CurrentUser(UUID id, String email, String fullName, @Nullable TicketCategory team, Set<String> roles) {

	public boolean hasRole(String role) {
		return this.roles.contains(role);
	}

	/** Agents only work tickets in their own team's category. */
	public boolean isAgentFor(TicketCategory category) {
		return hasRole(Roles.AGENT) && category == this.team;
	}

	public boolean is(AppUser user) {
		return this.id.equals(user.getId());
	}

}
