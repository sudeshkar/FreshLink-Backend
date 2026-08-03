package com.freshlink.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.freshlink.rating.dto.RatingResponse;
import com.freshlink.rating.dto.RatingRequest;

public interface RatingService {
	void rateSupplier(Long orderId,RatingRequest ratingRequest, String cafeEmail);

	/** Ratings this cafe has left, so it can review what it said. */
	Page<RatingResponse> getMyRatings(String cafeEmail, Pageable pageable);
	 
}
