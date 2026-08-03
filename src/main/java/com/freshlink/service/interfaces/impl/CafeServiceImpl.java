package com.freshlink.service.interfaces.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.enums.OrderStatus;
import com.freshlink.exception.BusinessRuleException;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.fishdto.FishMarketResponse;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.model.Cafe;
import com.freshlink.model.Fish;
import com.freshlink.model.Order;
import com.freshlink.model.OrderItem;
import com.freshlink.model.Supplier;
import com.freshlink.orderdto.OrderCreateRequest;
import com.freshlink.orderdto.OrderItemDto;
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
		return fishRepository.searchMarket(blankToNull(fishType), blankToNull(city))
				.stream()
				.map(fishMapper::toFishMarket)
				.collect(Collectors.toList());
	}

	/** An omitted filter and an empty one mean the same thing to the caller. */
	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	@Override
	public OrderResponse placeOrder(OrderCreateRequest dto, String email) {
		
		Cafe cafe = cafeRepository.findByEmail(email)
	            .orElseThrow(() -> new ResourceNotFoundException("Cafe", email));

	    if (dto.items() == null || dto.items().isEmpty()) {
	        throw new BusinessRuleException("Order must contain at least one item");
	    }

	    Order order = new Order();
	    order.setCafe(cafe);

	    Supplier orderSupplier = null;
	    List<OrderItem> items = new ArrayList<>();

	    for (OrderItemDto itemDto : dto.items()) {

	        Fish fish = fishRepository.findById(itemDto.fishId())
	                .orElseThrow(() -> new ResourceNotFoundException("Fish", itemDto.fishId()));

	        Supplier fishSupplier = fish.getSupplier();

	        // A removed or suspended supplier is off the market, so it must not be
	        // possible to order from one by passing a fish id directly.
	        if (fishSupplier.getDeletedAt() != null || !fishSupplier.isActive()) {
	            throw new BusinessRuleException(
	                    "This supplier is no longer accepting orders: " + fishSupplier.getName());
	        }

	        if (orderSupplier == null) {
	            orderSupplier = fishSupplier;
	            order.setSupplier(orderSupplier);
	        } else if (!orderSupplier.getId().equals(fishSupplier.getId())) {
	            throw new BusinessRuleException("Order cannot contain fish from multiple suppliers");
	        }

	        if (itemDto.quantityKg() <= 0) {
	            throw new BusinessRuleException("Quantity must be greater than zero");
	        }

	        if (fish.getAvailableKg() < itemDto.quantityKg()) {
	            throw new BusinessRuleException(
	                    "Not enough stock for fish: " + fish.getName()
	            );
	        }

	        // Reserve stock
	        fish.setAvailableKg(fish.getAvailableKg() - itemDto.quantityKg());
	        fish.setReservedKg(fish.getReservedKg() + itemDto.quantityKg());

	        OrderItem item = new OrderItem();
	        item.setOrder(order);
	        item.setFish(fish);
	        item.setQuantityKg(itemDto.quantityKg());
	        item.setPricePerKg(fish.getPricePerKg());

	        items.add(item);
	    }

	    order.setItems(items);

	    // No try/catch around save(): the @Version conflict on Fish is only detected
	    // at flush/commit, which happens after this method returns. Spring surfaces it
	    // as OptimisticLockingFailureException, which GlobalExceptionHandler turns
	    // into a 409 telling the cafe to retry.
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

	@Override
	public void cancelOrder(Long orderId,String cafeEmail) {
			Cafe cafe = cafeRepository.findByEmail(cafeEmail)
		            .orElseThrow(() -> new ResourceNotFoundException("Cafe", cafeEmail));

		    Order order = orderRepository.findById(orderId)
		            .filter(o -> o.getCafe().getId().equals(cafe.getId()))
		            // 404 rather than 403 on an ownership mismatch: a 403 would confirm
		            // that another cafe's order id exists.
		            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		    if (order.getStatus() != OrderStatus.REQUESTED) {
		        throw new BusinessRuleException("Only orders still awaiting supplier response can be cancelled");
		    }
		    
		    for (OrderItem item : order.getItems()) {
		        Fish fish = item.getFish();

		        fish.setAvailableKg(
		            fish.getAvailableKg() + item.getQuantityKg()
		        );
		        fish.setReservedKg(
		            fish.getReservedKg() - item.getQuantityKg()
		        );
		    }
		    
		    order.setStatus(OrderStatus.CANCELLED);
		
	}

}
