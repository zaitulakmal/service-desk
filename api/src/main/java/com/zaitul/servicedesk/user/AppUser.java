package com.zaitul.servicedesk.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

import com.zaitul.servicedesk.ticket.TicketCategory;

/**
 * A person in the company directory. Keycloak owns their login and roles; this row
 * holds what the service desk needs on top: their manager and support team.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	private UUID id;

	@Column(unique = true, length = 64)
	private @Nullable String keycloakId;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(nullable = false, length = 200)
	private String fullName;

	@Column(length = 100)
	private @Nullable String department;

	@Column(length = 100)
	private @Nullable String jobTitle;

	/** The support team an agent works in. Empty for everyone else. */
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private @Nullable TicketCategory team;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manager_id")
	private @Nullable AppUser manager;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "role", length = 50)
	private Set<String> roles = new HashSet<>();

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected AppUser() {
		// required by JPA
	}

	public AppUser(String email, String fullName) {
		this.id = UUID.randomUUID();
		this.email = email;
		this.fullName = fullName;
	}

	/** Copies identity details from the latest login so the directory never drifts from Keycloak. */
	void syncFromLogin(String keycloakId, String email, String fullName, Set<String> roles) {
		if (!Objects.equals(this.keycloakId, keycloakId)) {
			this.keycloakId = keycloakId;
		}
		if (!this.email.equals(email)) {
			this.email = email;
		}
		if (!this.fullName.equals(fullName)) {
			this.fullName = fullName;
		}
		if (!this.roles.equals(roles)) {
			this.roles.clear();
			this.roles.addAll(roles);
		}
	}

	public void setManager(@Nullable AppUser manager) {
		this.manager = manager;
	}

	public void setTeam(@Nullable TicketCategory team) {
		this.team = team;
	}

	public void setRoles(Set<String> roles) {
		this.roles.clear();
		this.roles.addAll(roles);
	}

	public void setJobTitle(@Nullable String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public void setDepartment(@Nullable String department) {
		this.department = department;
	}

	public UUID getId() {
		return this.id;
	}

	public @Nullable String getKeycloakId() {
		return this.keycloakId;
	}

	public String getEmail() {
		return this.email;
	}

	public String getFullName() {
		return this.fullName;
	}

	public @Nullable String getDepartment() {
		return this.department;
	}

	public @Nullable String getJobTitle() {
		return this.jobTitle;
	}

	public @Nullable TicketCategory getTeam() {
		return this.team;
	}

	public @Nullable AppUser getManager() {
		return this.manager;
	}

	public Set<String> getRoles() {
		return Set.copyOf(this.roles);
	}

}
