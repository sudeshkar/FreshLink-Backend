package com.freshlink.service.interfaces;

import com.freshlink.rating.dto.RatingRequest;

public interface RatingService {
	void rateSupplier(Long orderId,RatingRequest ratingRequest, String cafeEmail);
	 
}
