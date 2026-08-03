package com.freshlink.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freshlink.Repository.CafeRepository;
import com.freshlink.demand.dto.DemandRequestDto;
import com.freshlink.demand.dto.DemandResponse;
import com.freshlink.exception.ResourceNotFoundException;
import com.freshlink.model.Cafe;
import com.freshlink.service.interfaces.DemandService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/demand")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAFE')")
@Tag(name = "Demand", description = "Forward demand requests matched against supplier daily catch. Requires a CAFE token.")
public class DemandController {
	
	private final DemandService demandService;
	private final CafeRepository cafeRepository;
	
	@PostMapping("/create")
	public void createDemand( @RequestBody @Valid DemandRequestDto request, Authentication auth)
	{
		demandService.createDemand(request, currentCafe(auth));
	}

	@DeleteMapping("/{demandId}/delete")
	public void deleteDemand(@PathVariable Long demandId, Authentication auth) {
		demandService.deleteDemand(demandId, currentCafe(auth));
	}

	@GetMapping
	public List<DemandResponse> getMyDemand(Authentication auth){
		return demandService.getDemand(currentCafe(auth));
	}

	/** The cafe behind the bearer token - every operation here is scoped to it. */
	private Cafe currentCafe(Authentication auth) {
		return cafeRepository.findByEmail(auth.getName())
				.orElseThrow(() -> new ResourceNotFoundException("Cafe", auth.getName()));
	}

}
