package com.freshlink.enums;

public enum DeliveryStatus {
	/** Raised when the supplier marks the order as delivering. */
	SCHEDULED,
	/** Driver is on the way. */
	IN_TRANSIT,
	DELIVERED,
	/** Attempted but not completed - cafe closed, address wrong, goods rejected. */
	FAILED
}
