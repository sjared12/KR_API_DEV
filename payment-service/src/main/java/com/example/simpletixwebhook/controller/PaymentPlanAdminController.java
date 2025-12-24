package com.example.simpletixwebhook.controller;

import com.example.simpletixwebhook.model.PaymentPlan;
import com.example.simpletixwebhook.repository.PaymentPlanRepository;
import com.example.simpletixwebhook.service.PaymentPlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/plans")
public class PaymentPlanAdminController {
    private static final Logger log = LoggerFactory.getLogger(PaymentPlanAdminController.class);
    private final PaymentPlanRepository repo;
    private final PaymentPlanService service;

    public PaymentPlanAdminController(PaymentPlanRepository repo, PaymentPlanService service) {
        this.repo = repo;
        this.service = service;
    }

    @GetMapping
    public Page<PaymentPlan> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) PaymentPlan.Status status) {
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(200, size));
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (studentId != null && !studentId.isBlank() && status != null) {
            return repo.findByStudentIdAndStatus(studentId, status, pageable);
        } else if (studentId != null && !studentId.isBlank()) {
            return repo.findByStudentId(studentId, pageable);
        } else if (status != null) {
            return repo.findByStatus(status, pageable);
        } else {
            return repo.findAll(pageable);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentPlan> get(@PathVariable @org.springframework.lang.NonNull Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PaymentPlan> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        PaymentPlan plan = new PaymentPlan();
        plan.setStudentId(request.studentId());
        plan.setEmail(request.email());
        plan.setCadence(request.cadence());
        plan.setTotalDue(request.totalDue());
        plan.setInstallmentAmount(request.installmentAmount());
        plan.setRemaining(request.totalDue());
        plan.setStatus(PaymentPlan.Status.ACTIVE);
        plan.setInvoiceId(request.invoiceId());

        PaymentPlan saved = repo.save(plan);
        log.info("Created payment plan {} for student {} with total ${} and {} installments of ${}",
                saved.getId(), saved.getStudentId(), saved.getTotalDue(), 
                saved.getCadence(), saved.getInstallmentAmount());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<StatusResponse> completePlan(@PathVariable Long id) {
        boolean success = service.completePlan(id);
        if (success) {
            return ResponseEntity.ok(new StatusResponse("Plan marked complete"));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<StatusResponse> cancelPlan(
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest request) {
        
        String reason = request != null ? request.reason() : null;
        boolean success = service.cancelPlan(id, reason);
        if (success) {
            return ResponseEntity.ok(new StatusResponse("Plan canceled"));
        }
        return ResponseEntity.notFound().build();
    }

    // Request/Response DTOs
    public record CreatePlanRequest(
            @NotBlank String studentId,
            @NotBlank String email,
            @NotNull PaymentPlan.Cadence cadence,
            @NotNull BigDecimal totalDue,
            @NotNull BigDecimal installmentAmount,
            String invoiceId
    ) {}

    public record CancelRequest(String reason) {}
    public record StatusResponse(String message) {}
}
