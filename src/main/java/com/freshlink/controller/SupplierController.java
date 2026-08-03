package com.freshlink.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.fishdto.FishCreateRequest;
import com.freshlink.fishdto.FishResponse;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.orderdto.OrderResponse;
import com.freshlink.service.interfaces.SupplierService;
import com.freshlink.supplymatch.dto.SupplyMatchResponse;
import com.freshlink.userprofiledto.SupplierProfileResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPPLIER')")
public class SupplierController {
	
	private final SupplierService supplierService;
	
	@GetMapping("/me")
    public SupplierProfileResponse getMyProfile(Authentication auth) {
        return supplierService.getProfile(auth.getName());
    }

    @PostMapping("/fish")
    public FishResponse addFish(@RequestBody FishCreateRequest dto,
                                Authentication auth) {
        return supplierService.addFish(dto, auth.getName());
    }

    @GetMapping("/fish")
    public List<FishResponse> getMyFish(Authentication auth) {
        return supplierService.getMyFish(auth.getName());
    }

    @PutMapping("/fish/{id}")
    public FishResponse updateFish(
            @PathVariable Long id,
            @RequestBody FishUpdateRequest dto,
            Authentication auth) {
        return supplierService.updateFish(id, dto, auth.getName());
    }

    @DeleteMapping("/fish/{id}")
    public void deleteFish(@PathVariable Long id, Authentication auth) {
        supplierService.deleteFish(id, auth.getName());
    }
    
    @PutMapping("/orders/{orderId}/accept")
    public OrderResponse acceptOrder(@PathVariable Long orderId, Authentication auth) {
    	return supplierService.acceptOrder(orderId,auth.getName());
    }
    
    @GetMapping("/orders")
    public List<OrderResponse> getIncomingOrders(Authentication auth){
    	
    	return supplierService.getIncomingOrders(auth.getName());
    	
    }
    
    @PutMapping("/orders/{orderId}/reject")
    public void rejectOrder(@PathVariable Long orderId, Authentication auth) {
    	supplierService.rejectOrder(orderId,auth.getName());
    }
    
    @PutMapping("/orders/{orderId}/markdelivering")
    public void markDelivering(@PathVariable Long orderId,Authentication auth) {
    	supplierService.markDelivering(orderId,auth.getName());
    }
    
    @PutMapping("/orders/{orderId}/complete")
    public void completeOrder(@PathVariable Long orderId,Authentication auth) {
    	supplierService.completeOrder(orderId,auth.getName());
    }
    
    @GetMapping("/supply-matches")
    public List<SupplyMatchResponse> myMatches(Authentication auth) {
        return supplierService.getPendingMatches(auth.getName());
    }
    
    @PutMapping("/supply-matches/{id}/accept")
    public void accept(@PathVariable Long id, Authentication auth) {
        supplierService.acceptMatch(id, auth.getName());
    }
    
    @PutMapping("/supply-matches/{id}/reject")
    public void reject(@PathVariable Long id, Authentication auth) {
        supplierService.rejectMatch(id, auth.getName());
    }

    
    
    

}
