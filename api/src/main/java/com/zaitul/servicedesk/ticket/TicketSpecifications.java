package com.zaitul.servicedesk.ticket;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/** Reusable filters, combined at runtime depending on which query parameters were sent. */
final class TicketSpecifications {

	private static final Pattern REFERENCE = Pattern.compile("^(?:SD-)?0*(\\d{1,12})$", Pattern.CASE_INSENSITIVE);

	private TicketSpecifications() {
	}

	static Specification<Ticket> requestedBy(UUID userId) {
		return (root, query, cb) -> cb.equal(root.get("requester").get("id"), userId);
	}

	static Specification<Ticket> assignedTo(UUID userId) {
		return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
	}

	static Specification<Ticket> unassigned() {
		return (root, query, cb) -> cb.isNull(root.get("assignee"));
	}

	static Specification<Ticket> inCategory(TicketCategory category) {
		return (root, query, cb) -> cb.equal(root.get("category"), category);
	}

	static Specification<Ticket> hasStatus(TicketStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	static Specification<Ticket> statusIn(Collection<TicketStatus> statuses) {
		return (root, query, cb) -> root.get("status").in(statuses);
	}

	static Specification<Ticket> hasPriority(TicketPriority priority) {
		return (root, query, cb) -> cb.equal(root.get("priority"), priority);
	}

	/** Matches the title, or the ticket number when the text looks like "SD-1042" or "1042". */
	static Specification<Ticket> matches(String text) {
		String trimmed = text.trim();
		String pattern = "%" + trimmed.toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_") + "%";
		Matcher reference = REFERENCE.matcher(trimmed);
		return (root, query, cb) -> {
			Predicate title = cb.like(cb.lower(root.get("title")), pattern, '\\');
			return reference.matches() ? cb.or(title, cb.equal(root.get("id"), Long.parseLong(reference.group(1))))
					: title;
		};
	}

}
