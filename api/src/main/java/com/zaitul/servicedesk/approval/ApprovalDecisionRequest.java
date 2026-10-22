package com.zaitul.servicedesk.approval;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** A comment is optional when approving and required when rejecting. */
public record ApprovalDecisionRequest(@NotNull ApprovalDecision decision, @Size(max = 1000) @Nullable String comment) {
}
