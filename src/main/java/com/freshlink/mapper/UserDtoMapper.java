package com.freshlink.mapper;

import org.springframework.stereotype.Component;

import com.freshlink.model.User;
import com.freshlink.userprofiledto.UserDto;

@Component
public class UserDtoMapper {
	
	public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
