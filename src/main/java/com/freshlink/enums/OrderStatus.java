package com.freshlink.enums;

public enum OrderStatus {
	REQUESTED,   // Cafe placed order
    ACCEPTED,    // Supplier accepted
    REJECTED,    // Supplier rejected
    CANCELLED,   // Cafe cancelled
    DELIVERING,  // On the way
    COMPLETED    // Delivered & closed
}
