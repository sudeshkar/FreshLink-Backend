package com.freshlink.service.interfaces.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.freshlink.model.User;
import com.freshlink.security.UserPrincipal;
import com.freshlink.service.interfaces.UserService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
	
	private final UserService userService;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		  User user = userService.getByEmail(email);
		  
		  if (user == null) {
	            throw new UsernameNotFoundException("User not found");
	        }

	        return new UserPrincipal(user); 
	}
	
	
}
