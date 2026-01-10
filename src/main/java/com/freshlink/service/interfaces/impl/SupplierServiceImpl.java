package com.freshlink.service.interfaces.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.FishRepository;
import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.fishdto.FishCreateRequest;
import com.freshlink.fishdto.FishResponse;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.mapper.FishMapper;
import com.freshlink.mapper.SupplierMapper;
import com.freshlink.model.Fish;
import com.freshlink.model.FishType;
import com.freshlink.model.Supplier;
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
}
