package com.freshlink;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.FishTypeRepository;
import com.freshlink.Repository.UserRepository;
import com.freshlink.enums.Role;
import com.freshlink.model.Admin;
import com.freshlink.model.FishType;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrap {
	
	 	private final UserRepository userRepository;
	    private final PasswordEncoder passwordEncoder;
	    private final FishTypeRepository fishTypeRepository;

	    @PostConstruct
	    @Transactional
	    public void createAdminAndFishTypes() {

	     
	        if (!userRepository.existsByEmail("sudesh@gmail.com")) {
	            Admin admin = new Admin();
	            admin.setName("System Admin");
	            admin.setEmail("sudesh@gmail.com");
	            admin.setPhone("0000000000");
	            admin.setRole(Role.ADMIN);
	            admin.setPasswordHash(passwordEncoder.encode("12345"));
	            admin.setActive(true);
	            admin.setEmailVerified(true);

	            userRepository.save(admin);
	        }

	        
	        long count = fishTypeRepository.count();
	        System.out.println("FishType count = " + count);

	        if (count == 0) {
	            List<FishType> fishTypes = List.of(
	                new FishType(null, "Tuna", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31)),
	                new FishType(null, "Salmon", LocalDate.of(2025,3,1), LocalDate.of(2025,9,30)),
	                new FishType(null, "Prawns", LocalDate.of(2025,4,1), LocalDate.of(2025,11,30)),
	                new FishType(null, "Mackerel", LocalDate.of(2025,2,1), LocalDate.of(2025,8,31)),
	                new FishType(null, "Crab", LocalDate.of(2025,5,1), LocalDate.of(2025,10,31))
	            );

	            fishTypeRepository.saveAll(fishTypes);
	        }
	    }

	    
	    
	    
}
