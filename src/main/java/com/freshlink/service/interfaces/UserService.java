package com.freshlink.service.interfaces;

import java.util.List;

import org.springframework.security.core.Authentication;

import com.freshlink.model.User;
import com.freshlink.userprofiledto.UserDto;

public interface UserService {
	User createUser(User user);
    User getByEmail(String email);
    boolean existsByEmail(String email);
	List<User> getAllUsers();
	User getUserByEmail(String email);
	User getUserById(Long id);
	boolean checkUserExistByEmail(String email);
	boolean existsById(Long id);
	void deleteUser(Long id);
	
	UserDto getMyProfile(Authentication authentication);
}
