package com.freshlink.admindto;

public record CafeAdminResponse(
		 	Long id,
	        String name,
	        String email,
	        boolean active,
	        String phone
		) {

}
