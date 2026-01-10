package com.freshlink.mapper;

import org.springframework.stereotype.Component;

import com.freshlink.model.Supplier;
import com.freshlink.userprofiledto.SupplierProfileResponse;

@Component
public class SupplierMapper {
	
	public SupplierProfileResponse toProfile(Supplier supplier) {
        return new SupplierProfileResponse(
            supplier.getName(),
            supplier.getEmail(),
            supplier.getPhone(),
            supplier.isActive()
        );
    }

}
