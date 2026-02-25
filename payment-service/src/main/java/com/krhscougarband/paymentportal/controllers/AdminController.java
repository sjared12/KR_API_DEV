package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.dto.PlanDto;
import com.krhscougarband.paymentportal.dto.RefundRequest;
import com.krhscougarband.paymentportal.dto.RefundRecordDto;
import com.krhscougarband.paymentportal.entities.User;
import com.krhscougarband.paymentportal.repositories.UserRepository;
import com.krhscougarband.paymentportal.services.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','REFUND_APPROVER')")
public class AdminController {
    private final PlanService planService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PlanDto> getAllPlans() {
        return planService.getAllPlans();
    }

    @PostMapping("/plans/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refundPlan(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        try {
            long amountCents = ((Number) body.get("amount")).longValue();
            String method = (String) body.get("method");
            planService.refundPlan(id, amountCents, method);
            return ResponseEntity.ok(Map.of("message", "Refund submitted for approval"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("hasRole('REFUND_APPROVER')")
    public ResponseEntity<?> approveRefund(@PathVariable UUID refundId) {
        try {
            planService.approveRefund(refundId);
            return ResponseEntity.ok(Map.of("message", "Refund approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/refunds/pending")
    @PreAuthorize("hasRole('REFUND_APPROVER')")
    public List<Map<String, Object>> getPendingRefunds() {
        return planService.getPendingRefunds().stream().map(refund -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", refund.getId());
            map.put("planId", refund.getPlanId());
            map.put("refundAmount", refund.getRefundAmount());
            map.put("refundMethod", refund.getRefundMethod());
            map.put("requestedAt", refund.getRequestedAt());
            map.put("initiatedByUserEmail", refund.getInitiatedByUserEmail());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Pause a subscription (admin only)
     */
    @PostMapping("/plans/{planId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pausePlan(@PathVariable UUID planId) {
        try {
            planService.pausePlan(planId);
            return ResponseEntity.ok(Map.of("message", "Plan paused successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Resume a paused subscription (admin only)
     */
    @PostMapping("/plans/{planId}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resumePlan(@PathVariable UUID planId) {
        try {
            planService.resumePlan(planId);
            return ResponseEntity.ok(Map.of("message", "Plan resumed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Re-enable a cancelled plan (admin only)
     * POST /api/admin/plans/{planId}/reactivate
     */
    @PostMapping("/plans/{planId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reactivatePlan(@PathVariable UUID planId) {
        try {
            PlanDto plan = planService.reactivatePlan(planId);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("role", user.getRole());
            return map;
        }).collect(Collectors.toList());
    }
}
