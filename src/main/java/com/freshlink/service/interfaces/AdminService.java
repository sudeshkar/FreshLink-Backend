package com.freshlink.service.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.freshlink.admindto.CafeAdminResponse;
import com.freshlink.admindto.SupplierAdminResponse;

public interface AdminService {
	Page<SupplierAdminResponse> getSuppliers(Pageable pageable);
    
    Page<CafeAdminResponse> getCafes(Pageable pageable);
    
    void activateUser(Long userId);
    
    void deactivateUser(Long userId);

    /**
     * Soft-deletes an account. {@code actingAdminEmail} is the admin performing the
     * action, so the service can refuse self-deletion.
     */
    void deleteUser(Long userId, String actingAdminEmail);
}
