package com.freshlink.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.deliverydto.DeliveryResponse;
import com.freshlink.fishdto.PriceHistoryResponse;
import com.freshlink.enums.OrderStatus;
import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.rating.dto.RatingResponse;
import com.freshlink.rating.dto.RatingRequest;
import com.freshlink.service.interfaces.CafeService;
import com.freshlink.service.interfaces.RatingService;
import com.freshlink.userprofiledto.CafeProfileResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cafes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAFE')")
@Tag(name = "Cafe", description = "Marketplace browsing, ordering, and supplier ratings. Requires a CAFE token.")
public class CafeController {
	
	 	private final CafeService cafeService;
	 	private final RatingService ratingService;

	    @GetMapping("/me")
	    public CafeProfileResponse getProfile(Authentication auth) {
	        return cafeService.getProfile(auth.getName());
	    }

	    @GetMapping("/market/fish")
	    public Page<FishMarketResponse> browseFish(
	            @RequestParam(required = false) String fishType,
	            @RequestParam(required = false) String city,
	            @PageableDefault(size = 20) Pageable pageable) {
	        return cafeService.browseFish(fishType, city, pageable);
	    }

	    @PostMapping("/orders")
	    public OrderResponse placeOrder(
	            @RequestBody OrderCreateRequest dto,
	            Authentication auth) {
	        return cafeService.placeOrder(dto, auth.getName());
	    }

	    @GetMapping("/orders")
	    public Page<OrderResponse> myOrders(Authentication auth,
	            @RequestParam(required = false) OrderStatus status,
	            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
	        return cafeService.getOrders(auth.getName(), status, pageable);
	    }

	    @GetMapping("/ratings")
	    public Page<RatingResponse> myRatings(Authentication auth,
	            @PageableDefault(size = 20) Pageable pageable) {
	        return ratingService.getMyRatings(auth.getName(), pageable);
	    }
	    
	    @PutMapping("/orders/{orderId}/cancel")
	    public void cancelOrder(@PathVariable Long orderId,Authentication auth) {
	    	cafeService.cancelOrder(orderId,auth.getName());
	    }
	    
	    @GetMapping("/market/fish/{fishId}/price-history")
	    public Page<PriceHistoryResponse> priceHistory(
	            @PathVariable Long fishId,
	            @PageableDefault(size = 30) Pageable pageable) {
	        return cafeService.getPriceHistory(fishId, pageable);
	    }

	    @GetMapping("/orders/{orderId}/delivery")
	    public DeliveryResponse trackDelivery(@PathVariable Long orderId, Authentication auth) {
	        return cafeService.trackDelivery(orderId, auth.getName());
	    }

	    @PostMapping("/orders/{orderId}/rate")
	    public void rateSupplier(@PathVariable Long orderId,@RequestBody RatingRequest ratingRequest,Authentication auth) {
	    	ratingService.rateSupplier(orderId,ratingRequest,auth.getName());
	    }
}
