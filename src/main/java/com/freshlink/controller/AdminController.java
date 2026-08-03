package com.freshlink.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.admindto.CafeAdminResponse;
import com.freshlink.admindto.SupplierAdminResponse;
import com.freshlink.service.interfaces.AdminService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Account approval and lifecycle management. Requires an ADMIN token.")
public class AdminController {
	
	private final AdminService adminService;

    @GetMapping("/suppliers")
    public Page<SupplierAdminResponse> getAllSuppliers(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminService.getSuppliers(pageable);
    }

    @GetMapping("/cafes")
    public Page<CafeAdminResponse> getAllCafes(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminService.getCafes(pageable);
    }

    @PutMapping("/users/{id}/activate")
    public void activateUser(@PathVariable Long id) {
        adminService.activateUser(id);
    }
    
    @DeleteMapping("/users/{id}/delete")
    public void deleteUser(@PathVariable Long id, Authentication auth) {
    	adminService.deleteUser(id, auth.getName());
    }
}
