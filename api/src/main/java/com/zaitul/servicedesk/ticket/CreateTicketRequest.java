package com.zaitul.servicedesk.ticket;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** The requester is whoever is signed in, so it is not part of the request body. */
public record CreateTicketRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 5000) String description,
		@NotNull TicketType type,
		@NotNull TicketCategory category,
		@NotNull TicketPriority priority,
		@DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) @Nullable BigDecimal purchaseAmount) {
}
