package com.freshlink.service.interfaces;

import java.util.List;

import com.freshlink.admindto.CafeAdminResponse;
import com.freshlink.admindto.SupplierAdminResponse;

public interface AdminService {
	List<SupplierAdminResponse> getSuppliers();
    
    List<CafeAdminResponse> getCafes();
    
    void activateUser(Long userId);
    
    void deactivateUser(Long userId);
}
