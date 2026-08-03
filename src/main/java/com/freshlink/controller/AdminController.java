package com.freshlink.controller;

import java.util.List;

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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
	
	private final AdminService adminService;

    @GetMapping("/suppliers")
    public List<SupplierAdminResponse> getAllSuppliers() {
        return adminService.getSuppliers();
    }

    @GetMapping("/cafes")
    public List<CafeAdminResponse> getAllCafes() {
        return adminService.getCafes();
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
