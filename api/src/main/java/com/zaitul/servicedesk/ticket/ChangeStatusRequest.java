package com.zaitul.servicedesk.ticket;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull TicketStatus status) {
}
