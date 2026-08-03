package com.freshlink.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.freshlink.enums.DemandStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
    private Long id;

    @ManyToOne
    private Cafe cafe;

    @ManyToOne
    private FishType fishType;

    private Double requestedQuantity;
    
    private LocalDate requiredDate;

    private LocalDate weekStartDate;
    
    @PrePersist
    @PreUpdate
    private void calculateWeekStartDate() {
        if (requiredDate != null) {
            this.weekStartDate = requiredDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
        }
    }

    @Enumerated(EnumType.STRING)
    private DemandStatus status;
}
