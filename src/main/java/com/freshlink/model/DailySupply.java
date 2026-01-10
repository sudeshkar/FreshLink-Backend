package com.freshlink.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data  
@NoArgsConstructor 
@AllArgsConstructor
public class DailySupply {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplyId;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne
    private FishType fishType;

    private Double quantity;

    private LocalDateTime catchDateTime;

    private Double freshnessScore;
}
