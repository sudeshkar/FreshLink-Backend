package com.freshlink.service.interfaces.impl;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freshlink.Repository.UserRepository;
import com.freshlink.mapper.UserDtoMapper;
import com.freshlink.model.User;
import com.freshlink.service.interfaces.UserService;
import com.freshlink.userprofiledto.UserDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

	private final UserRepository userRepo;
	
	private final UserDtoMapper userDtoMapper;

	@Override
	public User createUser(User user) {
		 return userRepo.save(user);
	}

	@Override
	public User getByEmail(String email) {
		return userRepo.findByEmail(email).orElseThrow(()->new RuntimeException("User not found with email"));
              
	}

	@Override
	public boolean existsByEmail(String email) {
		 
		return userRepo.existsByEmail(email);
	}

	@Override
	public List<User> getAllUsers() {
		return userRepo.findAll();
	}

	@Override
	public User getUserByEmail(String email) {
		
		return userRepo.findByEmail(email).orElseThrow(()->new RuntimeException("User not found with email"));
                 
	}

	@Override
	public User getUserById(Long id) {
		return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
	}

	@Override
	public boolean checkUserExistByEmail(String email) {
		 
		return userRepo.existsByEmail(email);
	}
	@Override
	public boolean existsById(Long id) {
		return userRepo.existsById(id);
	}

	@Override
	@Transactional
	public void deleteUser(Long id) {
		if (!userRepo.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepo.deleteById(id);
		
	}

	@Override
	public UserDto getMyProfile(Authentication authentication) {
		String email = authentication.getName();
		User user = userRepo.findByEmail(email).orElseThrow(()->new RuntimeException("User not found Auth Failed"));
		 
		return userDtoMapper.toDto(user);
		
		 
	}

}
