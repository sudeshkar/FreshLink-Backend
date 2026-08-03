package com.freshlink.routedto;

import com.freshlink.enums.RouteStatus;

import jakarta.validation.constraints.NotNull;

public record RouteStatusUpdateRequest(
		@NotNull(message = "Status is required")
		RouteStatus status
) {
}
