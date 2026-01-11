package com.freshlink.model;

import com.freshlink.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("SUPPLIER")
@Getter
@Setter
@NoArgsConstructor
public class Supplier extends User {

    @Column(nullable = false)
    private String location;

    private String licenseNumber;
    
    private Double averageRating;
    private Integer ratingCount;

    
    @PrePersist
    @PreUpdate
    private void ensureRole() {
        this.role = Role.SUPPLIER;
    }

}
