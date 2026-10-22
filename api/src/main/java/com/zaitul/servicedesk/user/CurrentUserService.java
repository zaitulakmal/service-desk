package com.zaitul.servicedesk.user;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the directory entry for a token, creating it on first login ("just-in-time
 * provisioning"). People pre-loaded into the directory are matched by email.
 */
@Service
public class CurrentUserService {

	private static final String ROLE_PREFIX = "ROLE_";

	private final AppUserRepository users;

	CurrentUserService(AppUserRepository users) {
		this.users = users;
	}

	@Transactional
	public CurrentUser resolve(JwtAuthenticationToken authentication) {
		Jwt jwt = authentication.getToken();
		String email = jwt.getClaimAsString("email");
		if (email == null) {
			throw new AccessDeniedException("The access token has no email claim");
		}
		String name = jwt.getClaimAsString("name");
		String fullName = (name != null) ? name : email;
		Set<String> roles = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.filter((authority) -> authority.startsWith(ROLE_PREFIX))
			.map((authority) -> authority.substring(ROLE_PREFIX.length()))
			.collect(Collectors.toSet());

		AppUser user = this.users.findByKeycloakId(jwt.getSubject())
			.or(() -> this.users.findByEmailIgnoreCase(email))
			.orElseGet(() -> this.users.save(new AppUser(email, fullName)));
		user.syncFromLogin(jwt.getSubject(), email, fullName, roles);
		return new CurrentUser(user.getId(), user.getEmail(), user.getFullName(), user.getTeam(), roles);
	}

}
