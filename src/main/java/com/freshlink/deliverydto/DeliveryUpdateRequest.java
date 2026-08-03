package com.freshlink.deliverydto;

import java.time.LocalDateTime;

import com.freshlink.enums.DeliveryStatus;

import jakarta.validation.constraints.Size;

/**
 * Supplier-side update. Every field is optional: a dispatcher may assign a
 * driver first and move the status later, or do both at once.
 */
public record DeliveryUpdateRequest(

		DeliveryStatus status,

		@Size(max = 255, message = "Driver name is too long")
		String driverName,

		@Size(max = 255, message = "Driver phone is too long")
		String driverPhone,

		LocalDateTime expectedAt,

		@Size(max = 500, message = "Notes cannot exceed 500 characters")
		String notes
) {
}
