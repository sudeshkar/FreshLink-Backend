package com.freshlink.enums;

public enum DemandStatus {
	OPEN,           // created, waiting for matching
    PARTIALLY_MATCHED,
    FULLY_MATCHED,  // enough supply matched (but not accepted yet)
    FULFILLED,      // delivery completed
    CANCELLED
}
