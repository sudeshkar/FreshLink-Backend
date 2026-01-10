package com.freshlink.service.interfaces.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.enums.OrderStatus;
import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.model.Cafe;
import com.freshlink.model.Fish;
import com.freshlink.model.Order;
import com.freshlink.model.OrderItem;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.service.interfaces.CafeService;
import com.freshlink.userprofiledto.CafeProfileResponse;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class CafeServiceImpl implements CafeService{
	
	private final CafeRepository cafeRepository;
    private final FishRepository fishRepository;
    private final FishMapper fishMapper;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
	
	@Override
	public CafeProfileResponse getProfile(String name) {
		Cafe cafe = cafeRepository.findByEmail(name)
                .orElseThrow(() -> new RuntimeException("Cafe not found"));
        return new CafeProfileResponse(cafe.getName(), cafe.getEmail(), cafe.getPhone(), cafe.isActive());
	}

	@Override
	public List<FishMarketResponse> browseFish(String fishType, String city) {
		List<Fish> fishList;
        if (fishType != null) {
            fishList = fishRepository.findByFishType_Name(fishType);
        } else {
            fishList = fishRepository.findAll();
        }
        return fishList.stream()
                .map(fishMapper::toFishMarket)
                .collect(Collectors.toList());
	}

	@Override
	public OrderResponse placeOrder(OrderCreateRequest dto, String email) {
		Cafe cafe = cafeRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("Cafe not found"));
		
		 Order order = new Order();
		    order.setCafe(cafe);
		    order.setStatus(OrderStatus.CREATED);
		    
		    List<OrderItem> items = dto.items().stream().map(itemDto -> {

		        Fish fish = fishRepository.findById(itemDto.fishId())
		                .orElseThrow(() -> new RuntimeException("Fish not found"));

		        if (fish.getAvailableKg() < itemDto.quantityKg()) {
		            throw new RuntimeException(
		                "Not enough stock for fish: " + fish.getName()
		            );
		        }

		        
		        fish.setAvailableKg(
		            fish.getAvailableKg() - itemDto.quantityKg()
		        );

		        OrderItem item = new OrderItem();
		        item.setOrder(order);
		        item.setFish(fish);
		        item.setQuantityKg(itemDto.quantityKg());
		        item.setPricePerKg(fish.getPricePerKg());

		        return item;

		    }).toList();

		    order.setItems(items);

		    Order savedOrder = orderRepository.save(order);

		    return orderMapper.toOrderResponse(savedOrder);
		
	}

	@Override
	public List<OrderResponse> getOrders(String email) {
		Cafe cafe = cafeRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("Cafe not found"));
		
		 return orderRepository.findByCafe(cafe)
		            .stream()
		            .map(orderMapper::toOrderResponse)
		            .collect(Collectors.toList());
	}

}
