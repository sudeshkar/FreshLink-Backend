package com.freshlink.model;



import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.freshlink.enums.MatchStatus;

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
public class SupplyMatch {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private DemandRequest demandRequest;

    @ManyToOne
    private DailySupply dailySupply;

    private Double confirmedQuantity;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    /**
     * Pending matches hold a claim on their supply, so one a supplier never
     * answers has to be aged out or that quantity is locked away for good.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

}
