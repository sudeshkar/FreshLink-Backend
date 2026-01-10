package com.freshlink.userprofiledto;

import com.freshlink.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
	private Long id;
	private String email;
	private  Role role;
}
