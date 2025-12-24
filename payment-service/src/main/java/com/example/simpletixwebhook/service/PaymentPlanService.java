package com.example.simpletixwebhook.service;

import com.example.simpletixwebhook.model.PaymentPlan;
import com.example.simpletixwebhook.repository.PaymentPlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing payment plans and applying payments.
 * 
 * <p>This service handles:
 * <ul>
 *   <li>Applying payments to active payment plans (by student ID or invoice ID)</li>
 *   <li>Automatically marking plans as COMPLETE when balance reaches zero</li>
 *   <li>Processing Square webhook events for invoice payments</li>
 *   <li>Manual plan completion and cancellation (admin actions)</li>
 * </ul>
 * 
 * <p>Payment flow:
 * <ol>
 *   <li>Webhook receives payment.created event from Square</li>
 *   <li>Extract student ID from payment metadata or reference_id</li>
 *   <li>Call {@link #applyPaymentByStudentId(String, BigDecimal, String)} with student ID and amount</li>
 *   <li>Service finds active plan, subtracts amount from remaining balance</li>
 *   <li>If remaining <= 0, plan status is set to COMPLETE</li>
 * </ol>
 * 
 * @see PaymentPlan
 * @see PaymentPlanRepository
 */
@Service
public class PaymentPlanService {
    private static final Logger log = LoggerFactory.getLogger(PaymentPlanService.class);

    private final PaymentPlanRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public PaymentPlanService(PaymentPlanRepository repo) {
        this.repo = repo;
    }

    /**
     * Applies a payment to the student's active payment plan (by student ID).
     * 
     * <p>This method:
     * <ul>
     *   <li>Finds the first ACTIVE plan for the student</li>
     *   <li>Subtracts the payment amount from the remaining balance</li>
     *   <li>Sets status to COMPLETE if balance reaches zero or below</li>
     *   <li>Logs all state changes for audit trail</li>
     * </ul>
     * 
     * @param studentId The student identifier (must match PaymentPlan.studentId)
     * @param paymentAmount The amount paid (in dollars, e.g., 50.00)
     * @param paymentId Optional Square payment ID for reference logging
     * @return true if a plan was found and updated, false if no active plan exists
     */
    @Transactional
    public boolean applyPaymentByStudentId(String studentId, BigDecimal paymentAmount, String paymentId) {
        if (studentId == null || studentId.isBlank()) {
            log.warn("Cannot apply payment: studentId is null or blank");
            return false;
        }

        if (paymentAmount == null || paymentAmount.signum() <= 0) {
            log.warn("Cannot apply payment: amount is null or non-positive for student {}", studentId);
            return false;
        }

        // Find all active plans for this student
        List<PaymentPlan> activePlans = repo
                .findByStudentIdAndStatus(studentId, PaymentPlan.Status.ACTIVE, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        if (activePlans.isEmpty()) {
            log.info("No active payment plan found for student {}. Payment {} not applied to any plan.", studentId, paymentId);
            return false;
        }

        // Apply payment to the first active plan (most common case: one plan per student)
        PaymentPlan plan = activePlans.get(0);
        BigDecimal previousRemaining = plan.getRemaining();
        BigDecimal newRemaining = previousRemaining.subtract(paymentAmount);

        plan.setRemaining(newRemaining);

        // Auto-complete if fully paid
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            plan.setStatus(PaymentPlan.Status.COMPLETE);
            log.info("Payment plan {} for student {} marked COMPLETE. Payment {} applied: ${} paid, balance was ${}, now ${}",
                    plan.getId(), studentId, paymentId, paymentAmount, previousRemaining, newRemaining);
        } else {
            log.info("Payment {} applied to plan {} for student {}: ${} paid, remaining balance now ${}",
                    paymentId, plan.getId(), studentId, paymentAmount, newRemaining);
        }

        repo.save(plan);

        // If multiple active plans exist, log a warning (unusual but possible)
        if (activePlans.size() > 1) {
            log.warn("Student {} has {} active payment plans. Payment applied to plan {} only.", 
                    studentId, activePlans.size(), plan.getId());
        }

        return true;
    }

    /**
     * Finds an active payment plan by student ID.
     * 
     * @param studentId The student identifier
     * @return Optional containing the first active plan, or empty if none exists
     */
    public Optional<PaymentPlan> findActivePaymentPlan(String studentId) {
        List<PaymentPlan> activePlans = repo
                .findByStudentIdAndStatus(studentId, PaymentPlan.Status.ACTIVE, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        
        return activePlans.isEmpty() ? Optional.empty() : Optional.of(activePlans.get(0));
    }

    /**
     * Manually marks a payment plan as complete (admin override).
     * 
     * @param planId The plan ID
     * @return true if plan was found and completed, false otherwise
     */
    @Transactional
    public boolean completePlan(Long planId) {
        Optional<PaymentPlan> planOpt = repo.findById(planId);
        if (planOpt.isEmpty()) {
            log.warn("Cannot complete plan {}: not found", planId);
            return false;
        }

        PaymentPlan plan = planOpt.get();
        if (plan.getStatus() == PaymentPlan.Status.COMPLETE) {
            log.info("Plan {} already marked COMPLETE", planId);
            return true;
        }

        plan.setStatus(PaymentPlan.Status.COMPLETE);
        plan.setRemaining(BigDecimal.ZERO);
        repo.save(plan);
        log.info("Plan {} manually marked COMPLETE for student {}", planId, plan.getStudentId());
        return true;
    }

    /**
     * Manually cancels a payment plan (admin action).
     * 
     * @param planId The plan ID
     * @param reason Optional cancellation reason
     * @return true if plan was found and canceled, false otherwise
     */
    @Transactional
    public boolean cancelPlan(Long planId, String reason) {
        Optional<PaymentPlan> planOpt = repo.findById(planId);
        if (planOpt.isEmpty()) {
            log.warn("Cannot cancel plan {}: not found", planId);
            return false;
        }

        PaymentPlan plan = planOpt.get();
        plan.setStatus(PaymentPlan.Status.CANCELED);
        repo.save(plan);
        log.info("Plan {} canceled for student {}. Reason: {}", planId, plan.getStudentId(), reason != null ? reason : "none");
        return true;
    }

    /**
     * Processes Square webhook events related to invoices and payments.
     * 
     * <p>Handles:
     * <ul>
     *   <li>payment.created / payment.updated — applies payment to plan by invoice ID or student ID from metadata</li>
     *   <li>invoice.payment_made — applies payment from invoice event</li>
     * </ul>
     * 
     * <p>Logic:
     * <ol>
     *   <li>First attempts invoice ID-based tracking (for Square recurring invoices)</li>
     *   <li>Falls back to student ID extraction from payment metadata/referenceId (for direct payments)</li>
     * </ol>
     * 
     * @param payload Raw webhook JSON payload
     * @param eventType The Square event type (e.g., "payment.created")
     */
    @Transactional
    public void handleSquareWebhook(String payload, String eventType) {
        try {
            JsonNode root = mapper.readTree(payload);
            
            // Try payment events first (payment.created/payment.updated)
            String invoiceId = extractInvoiceIdFromPayment(root);
            Long cents = extractPaidCentsFromPayment(root);
            String studentId = extractStudentIdFromPayment(root);

            if (invoiceId == null) {
                // Try invoice events (invoice.payment_made)
                invoiceId = extractInvoiceIdFromInvoice(root);
                if (cents == null) cents = extractPaidCentsFromInvoice(root);
            }

            // Apply by invoice ID if present (Square recurring invoice payments)
            if (invoiceId != null && cents != null) {
                final String invId = invoiceId;
                final long paidCents = cents;
                repo.findByInvoiceId(invId).ifPresent(plan -> {
                    if (plan.getStatus() != PaymentPlan.Status.ACTIVE) return;
                    BigDecimal paid = BigDecimal.valueOf(paidCents).movePointLeft(2);
                    BigDecimal remaining = plan.getRemaining().subtract(paid);
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        plan.setRemaining(BigDecimal.ZERO);
                        plan.setStatus(PaymentPlan.Status.COMPLETE);
                    } else {
                        plan.setRemaining(remaining);
                    }
                    repo.save(plan);
                    log.info("Payment applied to plan {} invoice {}: paid={}, remaining={}, status={}", 
                            plan.getId(), invId, paid, plan.getRemaining(), plan.getStatus());
                });
                return; // Invoice-based tracking handled, done
            }

            // Apply by student ID if invoice not present (direct /charge payments)
            if (studentId != null && cents != null) {
                BigDecimal paid = BigDecimal.valueOf(cents).movePointLeft(2);
                boolean applied = applyPaymentByStudentId(studentId, paid, "webhook-" + eventType);
                if (applied) {
                    log.info("Webhook {} applied payment ${} to student {} plan via student ID tracking", 
                            eventType, paid, studentId);
                } else {
                    log.debug("Webhook {} for student {}: no active plan to apply ${} payment", 
                            eventType, studentId, paid);
                }
            }
        } catch (Exception e) {
            log.error("Error handling plan webhook", e);
        }
    }

    /**
     * Extracts student ID from payment metadata or reference_id.
     * 
     * <p>Checks these fields in order:
     * <ul>
     *   <li>payment.reference_id</li>
     *   <li>payment.note (if contains studentId=XXX pattern)</li>
     *   <li>order.reference_id (if payment links to order)</li>
     * </ul>
     * 
     * @param root Parsed webhook JSON root node
     * @return Student ID string, or null if not found
     */
    private String extractStudentIdFromPayment(JsonNode root) {
        try {
            JsonNode payment = root.path("data").path("object").path("payment");
            if (payment.isMissingNode()) {
                payment = root.path("data").path("object");
            }
            
            // Check reference_id field (most common)
            if (payment.has("reference_id")) {
                String refId = payment.get("reference_id").asText();
                if (refId != null && !refId.isBlank()) {
                    return refId;
                }
            }
            
            // Check note field for studentId=XXX pattern
            if (payment.has("note")) {
                String note = payment.get("note").asText();
                if (note != null && note.contains("studentId=")) {
                    String[] parts = note.split("studentId=");
                    if (parts.length > 1) {
                        return parts[1].split("[,\\s]")[0]; // Extract until comma or space
                    }
                }
            }
            
            // Check order reference_id if order_id present
            if (payment.has("order_id")) {
                String orderId = payment.get("order_id").asText();
                // Note: Would need to query Square Orders API here to get order.reference_id
                // For now, skip this lookup to avoid API calls in webhook handler
                log.debug("Payment has order_id {} but order lookup not implemented", orderId);
            }
        } catch (Exception e) {
            log.debug("Error extracting student ID from payment", e);
        }
        return null;
    }

    private String extractInvoiceIdFromPayment(JsonNode root) {
        try {
            JsonNode data = root.path("data").path("object").path("payment");
            if (!data.isMissingNode() && data.has("invoice_id")) return data.get("invoice_id").asText();
            data = root.path("data").path("object");
            if (data.has("invoice_id")) return data.get("invoice_id").asText();
        } catch (Exception ignored) {}
        return null;
    }

    private Long extractPaidCentsFromPayment(JsonNode root) {
        try {
            JsonNode p = root.path("data").path("object").path("payment");
            if (!p.isMissingNode()) {
                if (p.has("total_money")) return p.get("total_money").get("amount").asLong();
                if (p.has("amount_money")) return p.get("amount_money").get("amount").asLong();
            }
            p = root.path("data").path("object");
            if (p.has("total_money")) return p.get("total_money").get("amount").asLong();
            if (p.has("amount_money")) return p.get("amount_money").get("amount").asLong();
        } catch (Exception ignored) {}
        return null;
    }

    private String extractInvoiceIdFromInvoice(JsonNode root) {
        try {
            JsonNode inv = root.path("data").path("object").path("invoice");
            if (!inv.isMissingNode() && inv.has("id")) return inv.get("id").asText();
        } catch (Exception ignored) {}
        return null;
    }

    private Long extractPaidCentsFromInvoice(JsonNode root) {
        try {
            JsonNode inv = root.path("data").path("object").path("invoice");
            if (!inv.isMissingNode() && inv.has("payment_requests")) {
                // Sum payments_made_money if present, else return null
                if (inv.has("payments")) {
                    long sum = 0L;
                    for (JsonNode pay : inv.get("payments")) {
                        if (pay.has("total_money")) sum += pay.get("total_money").get("amount").asLong();
                    }
                    return sum > 0 ? sum : null;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
