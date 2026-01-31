package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.dto.PlanDto;
import com.krhscougarband.paymentportal.dto.PlanUpdateDto;
import com.krhscougarband.paymentportal.entities.Plan;
import com.krhscougarband.paymentportal.services.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanDto> getMyPlans(Authentication auth) {
        if (auth == null) {
            return List.of(); // allow anonymous fetch to succeed
        }
        return planService.getPlansForUser(auth.getName());
    }

    @PostMapping
    public PlanDto createPlan(@Valid @RequestBody PlanDto dto, Authentication auth) {
        return planService.createPlan(dto, auth.getName());
    }

    @PutMapping("/{id}")
    public PlanDto updatePlan(@PathVariable UUID id, @RequestBody PlanUpdateDto dto, Authentication auth) {
        return planService.updatePlan(id, dto, auth.getName());
    }

    @DeleteMapping("/{id}")
    public void cancelPlan(@PathVariable UUID id, Authentication auth) {
        planService.cancelPlan(id, auth.getName());
    }

    @PutMapping("/{id}/frequency")
    public ResponseEntity<?> updateFrequency(@PathVariable UUID id, @RequestBody Map<String, String> body, Authentication auth) {
        String frequency = body.get("frequency");
        if (frequency == null || frequency.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Frequency is required"));
        }
        
        Plan plan = planService.updateFrequency(id, frequency, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Frequency updated successfully", "frequency", plan.getFrequency()));
    }
}
