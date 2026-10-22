package com.zaitul.notifications;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.zaitul.notifications.TicketEvent.Person;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Who gets an email for which event, and what it says. Pure logic with no I/O, so every
 * rule is easy to unit test. Nobody is emailed about something they did themselves.
 */
@Component
public class NotificationRules {

	/** The event types that can produce emails. */
	static final List<String> EVENT_TYPES = List.of("TICKET_CREATED", "APPROVAL_REQUESTED", "TICKET_APPROVED",
			"TICKET_REJECTED", "TICKET_ASSIGNED", "STATUS_CHANGED", "COMMENT_ADDED", "SLA_BREACHED");

	private final String webUrl;

	NotificationRules(@Value("${notifications.web-url}") String webUrl) {
		this.webUrl = webUrl;
	}

	public List<Email> emailsFor(TicketEvent event) {
		TicketEvent.TicketInfo ticket = event.ticket();
		String ref = ticket.reference();
		String actor = (event.actor() != null) ? event.actor().name() : "Someone";
		return switch (event.type()) {
			// The one email people get about their own action: a receipt for what they raised.
			case "TICKET_CREATED" -> emails(event, List.of(event.requester()), "We received your request " + ref,
					"Thanks, we have your request \"%s\". We will let you know when someone picks it up."
						.formatted(ticket.title()));
			case "APPROVAL_REQUESTED" -> to(event, event.audienceOrEmpty(),
					"Approval needed: " + ref + " " + ticket.title(),
					"%s asked for \"%s\" and it needs your approval (%s)."
						.formatted(event.requester().name(), ticket.title(), orEmpty(event.message())));
			case "TICKET_APPROVED" -> to(event, List.of(event.requester()), "Your request " + ref + " was approved",
					"Good news: \"%s\" has every approval it needs. The support team will take it from here."
						.formatted(ticket.title()));
			case "TICKET_REJECTED" -> to(event, List.of(event.requester()), "Your request " + ref + " was rejected",
					"%s rejected \"%s\".%nReason: %s".formatted(actor, ticket.title(), orEmpty(event.message())));
			case "TICKET_ASSIGNED" -> to(event, List.of(event.requester()), actor + " is working on " + ref,
					"%s has picked up \"%s\".".formatted(actor, ticket.title()));
			case "STATUS_CHANGED" -> to(event, List.of(event.requester()), ref + " was updated",
					"\"%s\" changed status: %s.".formatted(ticket.title(), orEmpty(event.message())));
			case "COMMENT_ADDED" -> to(event, commentRecipients(event), "New reply on " + ref,
					"%s wrote:%n%n%s".formatted(actor, orEmpty(event.message())));
			case "SLA_BREACHED" -> to(event, event.audienceOrEmpty(), "SLA breached: " + ref + " " + ticket.title(),
					"\"%s\" (%s priority) is past its resolution time and needs attention."
						.formatted(ticket.title(), ticket.priority().toLowerCase(Locale.ROOT)));
			default -> List.of();
		};
	}

	/** The requester hears about replies from others; the assignee hears about the requester's replies. */
	private static List<Person> commentRecipients(TicketEvent event) {
		List<Person> recipients = new ArrayList<>();
		recipients.add(event.requester());
		if (event.assignee() != null) {
			recipients.add(event.assignee());
		}
		return recipients;
	}

	/** Emails everyone listed except the person who caused the event. */
	private List<Email> to(TicketEvent event, List<Person> people, String subject, String text) {
		String actorEmail = (event.actor() != null) ? event.actor().email() : null;
		List<Person> others = people.stream()
			.filter((person) -> !person.email().equalsIgnoreCase(String.valueOf(actorEmail)))
			.toList();
		return emails(event, others, subject, text);
	}

	private List<Email> emails(TicketEvent event, List<Person> people, String subject, String text) {
		Map<String, Person> unique = new LinkedHashMap<>();
		people.forEach((person) -> unique.putIfAbsent(person.email().toLowerCase(Locale.ROOT), person));
		String link = this.webUrl + "/tickets/" + event.ticket().id();
		return unique.values()
			.stream()
			.map((person) -> new Email(person.email(), subject,
					"Hi %s,%n%n%s%n%nOpen the request: %s%n%n— Service Desk".formatted(firstName(person), text,
							link)))
			.toList();
	}

	private static String firstName(Person person) {
		return person.name().split(" ")[0];
	}

	private static String orEmpty(@Nullable String text) {
		return (text != null) ? text : "";
	}

}
