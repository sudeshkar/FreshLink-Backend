package com.freshlink.enums;

public enum RouteStatus {
	/** Being assembled - deliveries can still be added or removed. */
	PLANNED,
	/** Driver has left; every delivery on it moved to IN_TRANSIT. */
	DISPATCHED,
	/** Every stop has been attempted. */
	COMPLETED,
	CANCELLED
}
