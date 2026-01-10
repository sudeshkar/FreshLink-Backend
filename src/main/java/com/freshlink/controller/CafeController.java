package com.freshlink.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.service.interfaces.CafeService;
import com.freshlink.userprofiledto.CafeProfileResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cafes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAFE')")
public class CafeController {
	
	 	private final CafeService cafeService;

	    @GetMapping("/me")
	    public CafeProfileResponse getProfile(Authentication auth) {
	        return cafeService.getProfile(auth.getName());
	    }

	    @GetMapping("/market/fish")
	    public List<FishMarketResponse> browseFish(
	            @RequestParam(required = false) String fishType,
	            @RequestParam(required = false) String city) {
	        return cafeService.browseFish(fishType, city);
	    }

	    @PostMapping("/orders")
	    public OrderResponse placeOrder(
	            @RequestBody OrderCreateRequest dto,
	            Authentication auth) {
	        return cafeService.placeOrder(dto, auth.getName());
	    }

	    @GetMapping("/orders")
	    public List<OrderResponse> myOrders(Authentication auth) {
	        return cafeService.getOrders(auth.getName());
	    }
}
