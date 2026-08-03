package com.freshlink.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import com.freshlink.supplydto.DailySupplyCreateRequest;
import com.freshlink.supplydto.DailySupplyResponse;
import com.freshlink.supplydto.DailySupplyUpdateRequest;
import com.freshlink.supplymatch.dto.SupplyMatchResponse;
import com.freshlink.userprofiledto.SupplierProfileResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPPLIER')")
@Tag(name = "Supplier", description = "Fish listings, incoming orders, daily catch, and demand matches. Requires a SUPPLIER token.")
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
    public Page<OrderResponse> getIncomingOrders(Authentication auth,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable){
    	return supplierService.getIncomingOrders(auth.getName(), pageable);
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
    
    @Operation(summary = "Record today's catch",
            description = "Feeds the matching engine. Matching runs immediately on save, "
                    + "so a match may already exist by the time this returns.")
    @PostMapping("/daily-supply")
    public DailySupplyResponse addDailySupply(
            @RequestBody @Valid DailySupplyCreateRequest dto,
            Authentication auth) {
        return supplierService.addDailySupply(dto, auth.getName());
    }

    @GetMapping("/daily-supply")
    public List<DailySupplyResponse> myDailySupply(Authentication auth) {
        return supplierService.getMyDailySupply(auth.getName());
    }

    @PutMapping("/daily-supply/{id}")
    public DailySupplyResponse updateDailySupply(
            @PathVariable Long id,
            @RequestBody @Valid DailySupplyUpdateRequest dto,
            Authentication auth) {
        return supplierService.updateDailySupply(id, dto, auth.getName());
    }

    @DeleteMapping("/daily-supply/{id}")
    public void deleteDailySupply(@PathVariable Long id, Authentication auth) {
        supplierService.deleteDailySupply(id, auth.getName());
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
