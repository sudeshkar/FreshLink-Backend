package com.freshlink.admindto;

public record SupplierAdminResponse(
		Long id,
        String name,
        String email,
        boolean active,
        String phone
		) {

}
