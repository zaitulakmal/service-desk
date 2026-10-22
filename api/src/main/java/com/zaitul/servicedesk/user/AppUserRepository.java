package com.zaitul.servicedesk.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

	Optional<AppUser> findByKeycloakId(String keycloakId);

	Optional<AppUser> findByEmailIgnoreCase(String email);

	@Query("select u from AppUser u join u.roles r where r = :role order by u.fullName")
	List<AppUser> findAllByRole(String role);

}
