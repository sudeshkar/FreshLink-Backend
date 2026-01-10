package com.freshlink.service.interfaces;

import java.util.List;

import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.userprofiledto.CafeProfileResponse;

public interface CafeService {

	CafeProfileResponse getProfile(String name);

	List<FishMarketResponse> browseFish(String fishType, String city);

	OrderResponse placeOrder(OrderCreateRequest dto, String email);

	List<OrderResponse> getOrders(String email);

}
