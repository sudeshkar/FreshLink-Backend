package com.freshlink.model;



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
    private Long matchId;

    @ManyToOne
    private DemandRequest demandRequest;

    @ManyToOne
    private DailySupply dailySupply;

    private Double confirmedQuantity;

    @Enumerated(EnumType.STRING)
    
    private MatchStatus status;

}
