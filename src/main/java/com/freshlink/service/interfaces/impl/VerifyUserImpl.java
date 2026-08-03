package com.freshlink.service.interfaces.impl;

import org.springframework.stereotype.Service;

import com.freshlink.Repository.UserRepository;
import com.freshlink.service.interfaces.VerifyUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerifyUserImpl implements VerifyUserService{
	private final UserRepository userRepository;
	
	
	@Override
	public void verifyUser(String email) {
		
		 userRepository.findByEmail(email).orElseThrow(()-> new RuntimeException("User Not found"));
	 
	}

}
