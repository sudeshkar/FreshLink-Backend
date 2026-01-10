package com.freshlink.service.interfaces.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.Repository.SupplierRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.admindto.CafeAdminResponse;
import com.freshlink.admindto.SupplierAdminResponse;
import com.freshlink.service.interfaces.AdminService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {
	
	private final SupplierRepository supplierRepository;
    private final CafeRepository cafeRepository;
    private final UserRepository userRepository;
    
	@Override
	public List<SupplierAdminResponse> getSuppliers() {
		 return supplierRepository.findAll()
	                .stream()
	                .map(s -> new SupplierAdminResponse(s.getId(), s.getName(), s.getEmail(), s.isActive(), s.getPhone()))
	                .collect(Collectors.toList());
	}

	@Override
	public List<CafeAdminResponse> getCafes() {
		return cafeRepository.findAll()
                .stream()
                .map(c -> new CafeAdminResponse(c.getId(), c.getName(), c.getEmail(), c.isActive(), c.getPhone()))
                .collect(Collectors.toList()); 
	}

	@Override
	public void activateUser(Long userId) {
		userRepository.findById(userId).ifPresent(u -> {
            u.setActive(true);
            userRepository.save(u);
        });
		
	}

	@Override
	public void deactivateUser(Long userId) {
		 userRepository.findById(userId).ifPresent(u -> {
	            u.setActive(false);
	            userRepository.save(u);
	        });
		
	}

}
