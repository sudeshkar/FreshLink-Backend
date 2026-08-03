package com.freshlink.service.interfaces.impl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.freshlink.Repository.UserRepository;
import com.freshlink.authdto.AuthResponseDto;
import com.freshlink.authdto.CafeRegisterRequest;
import com.freshlink.authdto.LoginRequestDto;
import com.freshlink.authdto.RefreshTokenRequestDto;
import com.freshlink.authdto.SupplierRegisterRequest;
import com.freshlink.model.Cafe;
import com.freshlink.model.Supplier;
import com.freshlink.model.User;
import com.freshlink.security.UserPrincipal;
import com.freshlink.service.interfaces.AuthService;
import com.freshlink.service.interfaces.RefreshTokenService;
import com.freshlink.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
	private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
	@Override
	public AuthResponseDto login(LoginRequestDto dto) {
		
		Authentication authentication =
	            authenticationManager.authenticate(
	                new UsernamePasswordAuthenticationToken(
	                    dto.email(),
	                    dto.password()
	                ) 
	            );
		UserPrincipal principal =
		            (UserPrincipal) authentication.getPrincipal();
		
		User user = principal.getUser();
		
		if (!user.isActive()) {
	        throw new RuntimeException("Account is not active. Please wait for admin approval.");
	    }
		
		String accessToken = jwtUtil.generateToken(
                principal.getUsername(),
                principal.getUser().getRole()
        );
		
		String refreshToken = refreshTokenService.createToken(principal.getUsername());
		
		return new AuthResponseDto(
                accessToken,
                refreshToken,
                principal.getUser().getRole().name()
        );
	}

	@Override
	public AuthResponseDto refresh(RefreshTokenRequestDto dto) {
		// Rotation: the response carries a *new* refresh token and the one just
		// presented stops working. Clients must store what comes back.
		return refreshTokenService.rotate(dto.refreshToken());
	}

	@Override
	public void logout(String email) {
		refreshTokenService.deleteByEmail(email);
		
	}

	@Override
	public void registerCafe(CafeRegisterRequest dto) {
		 
		if (userRepository.existsByEmail(dto.email())) {
	        throw new RuntimeException("Email already exists");
	    }
		if (!dto.password().matches("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=]).{8,}$")) {
	        throw new RuntimeException("Weak password");
	    }
		
		Cafe cafe = new Cafe();
	    cafe.setName(dto.name());
	    cafe.setEmail(dto.email());
	    cafe.setPhone(dto.phone());
	    cafe.setPasswordHash(passwordEncoder.encode(dto.password()));
	    cafe.setActive(false); 
	    cafe.setEmailVerified(false);
	    cafe.setAddress(dto.address());
	    cafe.setBusinessRegNo(dto.businessRegNo());
	    userRepository.save(cafe);

		
	}

	@Override
	public void registerSupplier(SupplierRegisterRequest dto) {
		
		 
		
		 if (userRepository.existsByEmail(dto.email())) {
		        throw new RuntimeException("Email already exists");
		    }
		 if (!dto.password().matches("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=]).{8,}$")) {
		        throw new RuntimeException("Weak password");
		    }
		
		 Supplier supplier = new Supplier();
	    supplier.setName(dto.name());
	    supplier.setEmail(dto.email());
	    supplier.setPhone(dto.phone());
	    supplier.setActive(false);
	    supplier.setPasswordHash(passwordEncoder.encode(dto.password()));
	    supplier.setLocation(dto.location());
	    supplier.setLicenseNumber(dto.licenseNumber());
	    
	    userRepository.save(supplier);
		
	}

}
