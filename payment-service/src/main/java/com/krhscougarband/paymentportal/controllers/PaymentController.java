package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.entities.PaymentLog;
import com.krhscougarband.paymentportal.entities.Plan;
import com.krhscougarband.paymentportal.entities.User;
import com.krhscougarband.paymentportal.repositories.PaymentLogRepository;
import com.krhscougarband.paymentportal.repositories.PlanRepository;
import com.krhscougarband.paymentportal.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentLogRepository paymentLogRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserRepository userRepository;

    // Get payment history for a specific plan
    @GetMapping("/plan/{planId}")
    public ResponseEntity<?> getPlanPayments(Authentication auth, @PathVariable UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // Verify user owns this plan
        if (!plan.getOwner().getEmail().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized to view this plan"));
        }

        List<PaymentLog> payments = paymentLogRepository.findByPlan(plan);
        return ResponseEntity.ok(payments);
    }

    // Add payment (card or cash) to a plan
    @PostMapping("/plan/{planId}")
    public ResponseEntity<?> addPayment(
            Authentication auth,
            @PathVariable UUID planId,
            @RequestBody Map<String, Object> body) {

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // Check authorization - admin or plan owner
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = "ADMIN".equals(user.getRole());
        if (!isAdmin && !plan.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        }

        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String paymentMethod = (String) body.getOrDefault("paymentMethod", "CARD");
            String transactionId = (String) body.getOrDefault("transactionId", null);

            PaymentLog payment = new PaymentLog();
            payment.setPlan(plan);
            payment.setAmount(amount);
            payment.setPaymentMethod(paymentMethod);
            payment.setStatus("PAID");
            payment.setTransactionId(transactionId);
            payment.setTimestamp(LocalDateTime.now());

            paymentLogRepository.save(payment);

            // Update plan's amountPaid
            BigDecimal currentPaid = plan.getAmountPaid() != null ? plan.getAmountPaid() : BigDecimal.ZERO;
            plan.setAmountPaid(currentPaid.add(amount));
            plan.setUpdatedAt(LocalDateTime.now());
            planRepository.save(plan);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment added successfully",
                    "payment", payment
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Update credit card information for a plan
    @PutMapping("/plan/{planId}/card")
    public ResponseEntity<?> updateCardInfo(
            Authentication auth,
            @PathVariable UUID planId,
            @RequestBody Map<String, String> body) {

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // Verify user owns this plan
        if (!plan.getOwner().getEmail().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        }

        try {
            plan.setCardLast4(body.get("cardLast4"));
            plan.setCardBrand(body.get("cardBrand"));
            plan.setCardHolderName(body.get("cardHolderName"));
            plan.setUpdatedAt(LocalDateTime.now());

            planRepository.save(plan);

            return ResponseEntity.ok(Map.of("message", "Card information updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get card info for a plan
    @GetMapping("/plan/{planId}/card")
    public ResponseEntity<?> getCardInfo(Authentication auth, @PathVariable UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // Verify authorization
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = "ADMIN".equals(user.getRole());
        if (!isAdmin && !plan.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        }

        return ResponseEntity.ok(Map.of(
                "cardLast4", plan.getCardLast4() != null ? plan.getCardLast4() : "",
                "cardBrand", plan.getCardBrand() != null ? plan.getCardBrand() : "",
                "cardHolderName", plan.getCardHolderName() != null ? plan.getCardHolderName() : ""
        ));
    }

    // Admin only: Add cash payment to any plan
    @PostMapping("/admin/plan/{planId}/cash")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> addCashPayment(
            @PathVariable UUID planId,
            @RequestBody Map<String, Object> body) {

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String notes = (String) body.getOrDefault("notes", "");

            PaymentLog payment = new PaymentLog();
            payment.setPlan(plan);
            payment.setAmount(amount);
            payment.setPaymentMethod("CASH");
            payment.setStatus("PAID");
            payment.setTransactionId("CASH-" + UUID.randomUUID());
            payment.setTimestamp(LocalDateTime.now());

            paymentLogRepository.save(payment);

            // Update plan's amountPaid
            BigDecimal currentPaid = plan.getAmountPaid() != null ? plan.getAmountPaid() : BigDecimal.ZERO;
            plan.setAmountPaid(currentPaid.add(amount));
            plan.setUpdatedAt(LocalDateTime.now());
            planRepository.save(plan);

            return ResponseEntity.ok(Map.of(
                    "message", "Cash payment added successfully",
                    "payment", payment
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
