package com.zaitul.notifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.zaitul.notifications.TicketEvent.Person;
import com.zaitul.notifications.TicketEvent.TicketInfo;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRulesTests {

	private static final Person AINA = new Person("Aina Rahman", "aina@example.com");

	private static final Person DANIEL = new Person("Daniel Lee", "daniel@example.com");

	private static final Person FARAH = new Person("Farah Aziz", "farah@example.com");

	private final NotificationRules rules = new NotificationRules("http://servicedesk.localhost");

	@Test
	void requesterGetsAReceiptForTheirOwnNewRequest() {
		List<Email> emails = this.rules.emailsFor(event("TICKET_CREATED", AINA, null, List.of(), null));

		assertThat(emails).extracting(Email::to).containsExactly("aina@example.com");
		assertThat(emails.getFirst().subject()).isEqualTo("We received your request SD-1042");
	}

	@Test
	void requesterHearsWhenAnAgentPicksUpTheirTicket() {
		List<Email> emails = this.rules.emailsFor(event("TICKET_ASSIGNED", DANIEL, DANIEL, List.of(), null));

		assertThat(emails).singleElement().satisfies((email) -> {
			assertThat(email.to()).isEqualTo("aina@example.com");
			assertThat(email.subject()).isEqualTo("Daniel Lee is working on SD-1042");
			assertThat(email.body()).startsWith("Hi Aina,").contains("http://servicedesk.localhost/tickets/1042");
		});
	}

	@Test
	void approversAreAskedForTheirDecision() {
		List<Email> emails = this.rules
			.emailsFor(event("APPROVAL_REQUESTED", null, null, List.of(FARAH), "Manager approval"));

		assertThat(emails).extracting(Email::to).containsExactly("farah@example.com");
		assertThat(emails.getFirst().subject()).isEqualTo("Approval needed: SD-1042 Laptop screen flickering");
	}

	@Test
	void rejectionTellsTheRequesterWhy() {
		List<Email> emails = this.rules.emailsFor(event("TICKET_REJECTED", FARAH, null, List.of(), "Use the shared report"));

		assertThat(emails.getFirst().body()).contains("Reason: Use the shared report");
	}

	@Test
	void repliesGoToTheOtherSideOfTheConversation() {
		assertThat(this.rules.emailsFor(event("COMMENT_ADDED", DANIEL, DANIEL, List.of(), "Try a new cable")))
			.extracting(Email::to)
			.containsExactly("aina@example.com");
		assertThat(this.rules.emailsFor(event("COMMENT_ADDED", AINA, DANIEL, List.of(), "Still broken")))
			.extracting(Email::to)
			.containsExactly("daniel@example.com");
	}

	@Test
	void nobodyIsEmailedAboutTheirOwnAction() {
		// Daniel raised the ticket and also picked it up himself.
		TicketEvent event = new TicketEvent(UUID.randomUUID(), "TICKET_ASSIGNED", Instant.now(), ticket(), DANIEL,
				DANIEL, DANIEL, List.of(), null);

		assertThat(this.rules.emailsFor(event)).isEmpty();
	}

	@Test
	void unknownEventTypesAreIgnored() {
		assertThat(this.rules.emailsFor(event("SOMETHING_NEW", null, null, List.of(), null))).isEmpty();
	}

	private static TicketEvent event(String type, Person actor, Person assignee, List<Person> audience, String message) {
		return new TicketEvent(UUID.randomUUID(), type, Instant.now(), ticket(), actor, AINA, assignee, audience,
				message);
	}

	private static TicketInfo ticket() {
		return new TicketInfo(1042, "SD-1042", "Laptop screen flickering", "INCIDENT", "IT", "HIGH", "IN_PROGRESS");
	}

}
