package com.freshlink.service.interfaces.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.freshlink.Repository.RefreshTokenRepository;
import com.freshlink.model.RefreshToken;
import com.freshlink.model.User;
import com.freshlink.service.interfaces.RefreshTokenService;
import com.freshlink.service.interfaces.UserService;
import com.freshlink.util.JwtUtil;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService{
	 private final RefreshTokenRepository repo;
	 private final JwtUtil jwtUtil;
	 private final UserService userService;
	@Override
	public RefreshToken createToken(String email) {
		RefreshToken token = new RefreshToken();
        token.setEmail(email);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryTime(LocalDateTime.now().plusDays(7));

        return repo.save(token);
	}

	@Override
	public String refreshAccessToken(String refreshToken) {
		RefreshToken token = repo.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = userService.getByEmail(token.getEmail());

        return jwtUtil.generateToken(user.getEmail(), user.getRole());
	}

	@Override
	public void deleteByEmail(String email) {
		repo.deleteByEmail(email);
		
	}

}
