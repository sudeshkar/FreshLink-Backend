package com.freshlink.model;

import java.time.LocalDate;

import com.freshlink.enums.DemandStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data  
@NoArgsConstructor 
@AllArgsConstructor
public class DemandRequest {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long demandId;

    @ManyToOne
    private Cafe cafe;

    @ManyToOne
    private FishType fishType;

    private Double requestedQuantity;

    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    
    private DemandStatus status;
}
