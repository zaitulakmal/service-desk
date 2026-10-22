package com.zaitul.servicedesk;

import java.util.Set;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;

import com.zaitul.servicedesk.ticket.TicketCategory;
import com.zaitul.servicedesk.user.AppUser;
import com.zaitul.servicedesk.user.AppUserRepository;
import com.zaitul.servicedesk.user.Roles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts every test from an empty database holding a small company:
 * <pre>
 * Farah (Finance Manager) ── manages ── Aina (staff)
 * Siti (service manager)  ── manages ── Daniel (IT agent, IT Security approver)
 * Mei (HR agent), Hafiz (staff, no manager)
 * </pre>
 * {@link #as(AppUser)} signs a request in as that person with a fake Keycloak token.
 */
@IntegrationTest
public abstract class ApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected AppUserRepository users;

	protected AppUser aina;

	protected AppUser farah;

	protected AppUser daniel;

	protected AppUser mei;

	protected AppUser siti;

	protected AppUser hafiz;

	@BeforeEach
	void resetDatabase() {
		this.jdbc.execute("""
				TRUNCATE ticket, approval_step, ticket_comment, ticket_activity, outbox_event,
				         app_user_role, app_user RESTART IDENTITY CASCADE
				""");
		this.farah = person("farah@example.com", "Farah Aziz", null, null, Roles.STAFF);
		this.aina = person("aina@example.com", "Aina Rahman", this.farah, null, Roles.STAFF);
		this.siti = person("siti@example.com", "Siti Hajar", null, null, Roles.STAFF, Roles.SERVICE_MANAGER);
		this.daniel = person("daniel@example.com", "Daniel Lee", this.siti, TicketCategory.IT, Roles.STAFF,
				Roles.AGENT, Roles.SECURITY_APPROVER);
		this.mei = person("mei@example.com", "Mei Ling Tan", null, TicketCategory.HR, Roles.STAFF, Roles.AGENT);
		this.hafiz = person("hafiz@example.com", "Hafiz Osman", null, null, Roles.STAFF);
	}

	private AppUser person(String email, String name, AppUser manager, TicketCategory team, String... roles) {
		AppUser user = new AppUser(email, name);
		user.setManager(manager);
		user.setTeam(team);
		user.setRoles(Set.of(roles));
		return this.users.save(user);
	}

	/** Signs the request in as this person, with the roles they hold in Keycloak. */
	protected RequestPostProcessor as(AppUser user) {
		return jwt()
			.jwt((token) -> token.subject("kc-" + user.getEmail())
				.claim("email", user.getEmail())
				.claim("name", user.getFullName()))
			.authorities(user.getRoles()
				.stream()
				.map((role) -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
				.toList());
	}

	protected long createTicket(AppUser requester, String json) throws Exception {
		String body = this.mockMvc
			.perform(post("/api/tickets").with(as(requester)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.<Number>read(body, "$.id").longValue();
	}

	protected long createIncident(AppUser requester) throws Exception {
		return createTicket(requester, """
				{ "title": "Laptop screen flickering when docked",
				  "description": "Flickers every few seconds when connected to the dock.",
				  "type": "INCIDENT", "category": "IT", "priority": "HIGH" }
				""");
	}

	protected long createAccessRequest(AppUser requester) throws Exception {
		return createTicket(requester, """
				{ "title": "Access to SAP Finance module",
				  "description": "Read-only access for month-end reports.",
				  "type": "ACCESS", "category": "IT", "priority": "MEDIUM" }
				""");
	}

	protected ResultActions changeStatus(AppUser user, long ticketId, String newStatus) throws Exception {
		return this.mockMvc.perform(patch("/api/tickets/{id}/status", ticketId).with(as(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"status\": \"" + newStatus + "\" }"));
	}

	protected ResultActions decide(AppUser user, long ticketId, String decision, String comment) throws Exception {
		String commentJson = (comment != null) ? "\"" + comment + "\"" : "null";
		return this.mockMvc.perform(post("/api/tickets/{id}/approval", ticketId).with(as(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"decision\": \"" + decision + "\", \"comment\": " + commentJson + " }"));
	}

	protected long outboxCount(String type) {
		Long count = this.jdbc.queryForObject("select count(*) from outbox_event where type = ?", Long.class, type);
		return (count != null) ? count : 0;
	}

}
