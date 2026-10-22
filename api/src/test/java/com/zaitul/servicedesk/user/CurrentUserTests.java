package com.zaitul.servicedesk.user;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUserTests extends ApiTestSupport {

	@Test
	void firstLoginLinksTheDirectoryEntryByEmail() throws Exception {
		this.mockMvc.perform(get("/api/me").with(as(this.aina)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Aina Rahman"))
			.andExpect(jsonPath("$.managerName").value("Farah Aziz"))
			.andExpect(jsonPath("$.roles[0]").value("staff"));

		assertThat(this.users.findByKeycloakId("kc-aina@example.com")).isPresent();
	}

	@Test
	void someoneNotInTheDirectoryIsAddedOnFirstLogin() throws Exception {
		this.mockMvc
			.perform(get("/api/me").with(jwt().jwt((token) -> token.subject("kc-new")
				.claim("email", "new.joiner@example.com")
				.claim("name", "New Joiner")).authorities(new SimpleGrantedAuthority("ROLE_staff"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("New Joiner"));

		assertThat(this.users.findByEmailIgnoreCase("new.joiner@example.com")).get()
			.extracting(AppUser::getKeycloakId)
			.isEqualTo("kc-new");
	}

	@Test
	void rolesFollowKeycloak() throws Exception {
		// Keycloak no longer gives Mei the agent role; her next request updates the directory.
		this.mockMvc.perform(get("/api/me").with(jwt().jwt((token) -> token.subject("kc-mei@example.com")
			.claim("email", "mei@example.com")
			.claim("name", "Mei Ling Tan")).authorities(new SimpleGrantedAuthority("ROLE_staff"))));

		assertThat(this.users.findByEmailIgnoreCase("mei@example.com")).get()
			.extracting(AppUser::getRoles)
			.isEqualTo(Set.of("staff"));
	}

}
