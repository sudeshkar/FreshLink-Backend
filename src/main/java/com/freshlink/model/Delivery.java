package com.freshlink.model;

import java.time.LocalDateTime;

import com.freshlink.enums.DeliveryStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data  
@NoArgsConstructor 
@AllArgsConstructor
public class Delivery {
	
		@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long deliveryId;
	    
	    @OneToOne
	    private Order order;

	    private LocalDateTime deliveryDate;

	    @Enumerated(EnumType.STRING)
	    private DeliveryStatus status;

}
