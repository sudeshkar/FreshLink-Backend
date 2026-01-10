package com.freshlink.service.interfaces;

import java.util.List;

import com.freshlink.fishdto.FishCreateRequest;
import com.freshlink.fishdto.FishResponse;
import com.freshlink.fishdto.FishUpdateRequest;
import com.freshlink.userprofiledto.SupplierProfileResponse;

public interface SupplierService {
		FishResponse addFish(FishCreateRequest dto, String supplierEmail);

	    List<FishResponse> getMyFish(String supplierEmail);

	    FishResponse updateFish(Long id, FishUpdateRequest dto, String supplierEmail);

	    void deleteFish(Long id, String supplierEmail);

		SupplierProfileResponse getProfile(String name);
}
