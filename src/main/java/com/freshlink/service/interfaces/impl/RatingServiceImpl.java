package com.freshlink.service.interfaces.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.RatingRepository;
import com.freshlink.enums.OrderStatus;
import com.freshlink.model.Order;
import com.freshlink.model.Rating;
import com.freshlink.model.Supplier;
import com.freshlink.rating.dto.RatingRequest;
import com.freshlink.service.interfaces.RatingService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService{
	
	private final OrderRepository orderRepository;
	private final RatingRepository ratingRepository;

	@Override
	public void rateSupplier(Long orderId,RatingRequest ratingRequest,String cafeEmail) {
		Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
		if (!order.getCafe().getEmail().equals(cafeEmail)) {
	        // 404 rather than 403, matching every other ownership check: a 403 would
	        // confirm the order exists and let one cafe enumerate another's.
	        throw new ResourceNotFoundException("Order", orderId);
	    }
		
		if (order.getStatus() != OrderStatus.COMPLETED) {
	        throw new BusinessRuleException("Only a completed order can be rated");
	    }
		
		 if (ratingRepository.existsByOrder(order)) {
		        throw new BusinessRuleException("This order has already been rated");
		    }
		 
		 Rating rating = new Rating();
		    rating.setOrder(order);
		    rating.setCafe(order.getCafe());
		    rating.setSupplier(order.getSupplier());
		    rating.setScore(ratingRequest.score());
		    rating.setComment(ratingRequest.comment());
		    rating.setCreatedAt(LocalDateTime.now());

		    ratingRepository.save(rating);

		    updateSupplierRating(order.getSupplier());
	}
	
	private void updateSupplierRating(Supplier supplier) {

	    List<Rating> ratings =
	            ratingRepository.findBySupplier(supplier);

	    double avg = ratings.stream()
	            .mapToInt(Rating::getScore)
	            .average()
	            .orElse(0.0);

	    supplier.setAverageRating(avg);
	    supplier.setRatingCount(ratings.size());
	}


}
