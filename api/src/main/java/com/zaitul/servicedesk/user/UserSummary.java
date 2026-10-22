package com.zaitul.servicedesk.user;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** The few details about a person that other API responses embed. */
public record UserSummary(UUID id, String fullName, String email, @Nullable String jobTitle) {

	public static UserSummary of(AppUser user) {
		return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getJobTitle());
	}

	public static @Nullable UserSummary ofNullable(@Nullable AppUser user) {
		return (user != null) ? of(user) : null;
	}

}
