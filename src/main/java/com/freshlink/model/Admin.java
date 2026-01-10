package com.freshlink.model;

import com.freshlink.enums.Role;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("ADMIN")
@Getter
@Setter
@NoArgsConstructor
@Table(name = "admin_table") 
public class Admin extends User {
	
	@PrePersist
    @PreUpdate
    private void ensureRole() {
        this.role = Role.ADMIN;
    }
}
