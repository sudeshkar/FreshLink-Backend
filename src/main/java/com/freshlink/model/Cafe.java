package com.freshlink.model;

import com.freshlink.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("CAFE")
@Getter
@Setter
@NoArgsConstructor
public class Cafe extends User {

    @Column(nullable = false)
    private String address;

    private String businessRegNo;
    
    
    @PrePersist
    void assignRole() {
        this.role = Role.CAFE;
    }

    
}
