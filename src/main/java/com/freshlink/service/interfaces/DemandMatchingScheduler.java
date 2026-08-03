package com.freshlink.service.interfaces;

public interface DemandMatchingScheduler {

	/** Allocates available supply against demand that is still unfilled. */
	void autoMatchDemands();

	/**
	 * Releases matches a supplier never answered, and closes demand whose
	 * delivery date has passed.
	 */
	void expireStaleMatches();
}
