package com.freshlink.service.interfaces.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.OrderRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.enums.OrderStatus;
import com.freshlink.fishdto.FishCreateRequest;
import com.freshlink.fishdto.FishResponse;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.OrderMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Order;
import com.freshlink.model.OrderItem;
import com.freshlink.model.Supplier;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.service.interfaces.SupplierService;
import com.freshlink.userprofiledto.SupplierProfileResponse;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService{
	private final FishRepository fishRepository;
    private final SupplierRepository supplierRepository;
    private final FishMapper fishMapper;
    private final FishTypeRepository fishTypeRepository;
    private final SupplierMapper supplierMapper;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
	@Override
	
	public FishResponse addFish(FishCreateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
		FishType fishType = fishTypeRepository.findByNameIgnoreCase(dto.fishTypeName())
	            .orElseThrow(() -> new RuntimeException("Fish type not found"));
		
		if (fishRepository.existsBySupplierAndFishType(supplier, fishType)) {
		    throw new RuntimeException("Fish already exists for supplier");
		}

		
        Fish fish = new Fish();
        fish.setName(dto.name());
        fish.setPricePerKg(dto.pricePerKg());
        fish.setAvailableKg(dto.availableKg());
        fish.setFishType(fishType);    
        fish.setSupplier(supplier);

        Fish saved = fishRepository.save(fish);
        return fishMapper.toFishResponse(saved);
	}
	@Override
	public List<FishResponse> getMyFish(String supplierEmail) {
		 Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	                .orElseThrow(() -> new RuntimeException("Supplier not found"));
	        return fishRepository.findBySupplier(supplier)
	                .stream()
	                .map(fishMapper::toFishResponse) 
	                .collect(Collectors.toList());
	}
	@Override
	public FishResponse updateFish(Long id, FishUpdateRequest dto, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
		
		Fish fish = fishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fish not found"));
		
		 if (!fish.getSupplier().getId().equals(supplier.getId())) {
	            throw new RuntimeException("Unauthorized");
	        }
		 
		 if (dto.name() != null && !dto.name().isBlank()) {
		        fish.setName(dto.name());
		    }
		 if (dto.pricePerKg() != null) fish.setPricePerKg(dto.pricePerKg());
        if (dto.availableKg() != null) fish.setAvailableKg(dto.availableKg());
        if (dto.fishTypeName() != null && !dto.fishTypeName().isBlank()) {
            FishType fishType = fishTypeRepository
                    .findByNameIgnoreCase(dto.fishTypeName())
                    .orElseThrow(() -> new RuntimeException("Fish type not found"));

            fish.setFishType(fishType);
        }

        
        Fish updated = fishRepository.save(fish);
        return fishMapper.toFishResponse(updated);
}
	@Override
	public void deleteFish(Long id, String supplierEmail) {
		 Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	                .orElseThrow(() -> new RuntimeException("Supplier not found"));

	        Fish fish = fishRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Fish not found"));

	        if (!fish.getSupplier().getId().equals(supplier.getId())) {
	            throw new RuntimeException("Unauthorized");
	        }

	        fishRepository.delete(fish);
		
	}
	
	@Override
	public SupplierProfileResponse getProfile(String name) {
		 Supplier supplier = supplierRepository.findByEmail(name)
				 .orElseThrow(() -> new RuntimeException("Supplier not found"));
		 
		 return supplierMapper.toProfile(supplier);
	}
	
	@Override
	public OrderResponse acceptOrder(Long orderId, String supplierEmail) {
		Order order = validateSupplierOrder(orderId, supplierEmail);
		
		if (order.getStatus() != OrderStatus.REQUESTED) {
	        throw new RuntimeException("Order cannot be accepted");
	    }
		
		for (OrderItem item : order.getItems()) {
	        Fish fish = item.getFish();

	        fish.setReservedKg(
	            fish.getReservedKg() - item.getQuantityKg()
	        );
	         
	    }
		 order.setStatus(OrderStatus.ACCEPTED);
		 return orderMapper.toOrderResponse(order);
 
		
	}
	@Override
	public List<OrderResponse> getIncomingOrders(String supplierEmail) {
		 Supplier supplier = supplierRepository.findByEmail(supplierEmail)
		            .orElseThrow(() -> new RuntimeException("Supplier not found"));
		 return orderRepository.findBySupplier(supplier)
				 .stream()
				 .map(orderMapper::toOrderResponse) 
				 .collect(Collectors.toList());
	}
	@Override
	public void rejectOrder(Long orderId, String supplierEmail) {
		 Order order = validateSupplierOrder(orderId, supplierEmail);
		 
		 if (order.getStatus() != OrderStatus.REQUESTED) {
		        throw new RuntimeException("Order cannot be rejected");
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
		 order.setStatus(OrderStatus.REJECTED);

		  
		
	}
	@Override
	public void markDelivering(Long orderId, String supplierEmail) {
		Order order = validateSupplierOrder(orderId, supplierEmail);
		if (order.getStatus() != OrderStatus.ACCEPTED) {
	        throw new RuntimeException("Invalid state");
	    }
		
		order.setStatus(OrderStatus.DELIVERING);
		
	}
	
	@Override
	public void completeOrder(Long orderId, String supplierEmail) {
		
		Order order = validateSupplierOrder(orderId, supplierEmail);

	    if (order.getStatus() != OrderStatus.DELIVERING) {
	        throw new RuntimeException("Invalid state");
	    }

	    order.setStatus(OrderStatus.COMPLETED);
		
	}
	
	private Order validateSupplierOrder(Long orderId, String supplierEmail) {
		Supplier supplier = supplierRepository.findByEmail(supplierEmail)
	            .orElseThrow(() -> new RuntimeException("Supplier not found"));

	    Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new RuntimeException("Order not found"));

	    if (!order.getSupplier().getId().equals(supplier.getId())) {
	        throw new RuntimeException("Unauthorized");
	    }

	    return order;
	}
}
