package com.zaitul.servicedesk.user;

/** Realm roles defined in Keycloak (deploy/keycloak/servicedesk-realm.json). */
public final class Roles {

	public static final String STAFF = "staff";

	public static final String AGENT = "agent";

	public static final String SECURITY_APPROVER = "security-approver";

	public static final String SERVICE_MANAGER = "service-manager";

	private Roles() {
	}

	/** How a role-based approver is shown to people, e.g. "Waiting on IT Security". */
	public static String displayName(String role) {
		return switch (role) {
			case SECURITY_APPROVER -> "IT Security";
			case SERVICE_MANAGER -> "Service management";
			case AGENT -> "Support agent";
			default -> role;
		};
	}

}
