package com.example.simpletixwebhook.service;

import com.example.simpletixwebhook.model.Subscription;
import com.example.simpletixwebhook.model.SubscriptionRefund;
import com.example.simpletixwebhook.model.SubscriptionRefund.RefundStatus;
import com.example.simpletixwebhook.model.User;
import com.example.simpletixwebhook.repository.SubscriptionRefundRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for refund request management with approval workflow and fee deduction.
 */
@Service
@Transactional
public class RefundService {
    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final SubscriptionRefundRepository refundRepository;

    // Default refund processing fee (2.5%)
    private static final BigDecimal DEFAULT_PROCESSING_FEE_PERCENT = new BigDecimal("2.5");

    public RefundService(SubscriptionRefundRepository refundRepository) {
        this.refundRepository = refundRepository;
    }

    /**
     * Request a refund for a subscription
     */
    public SubscriptionRefund requestRefund(Subscription subscription, BigDecimal requestedAmount, 
                                           String reason, User requestedByUser) {
        // Validate requested amount
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        if (requestedAmount.compareTo(subscription.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed subscription amount");
        }

        SubscriptionRefund refund = new SubscriptionRefund(subscription, requestedAmount, reason, requestedByUser);
        refund.setStatus(RefundStatus.REQUESTED);

        SubscriptionRefund saved = refundRepository.save(refund);
        log.info("Refund requested: {} for subscription {}", saved.getId(), subscription.getId());
        return saved;
    }

    /**
     * Get refund by ID
     */
    public Optional<SubscriptionRefund> getRefundById(Long id) {
        return refundRepository.findById(id);
    }

    /**
     * Get all refunds for a subscription
     */
    public List<SubscriptionRefund> getSubscriptionRefunds(Subscription subscription) {
        return refundRepository.findBySubscription(subscription);
    }

    /**
     * Get all refunds for a subscription with pagination
     */
    public Page<SubscriptionRefund> getSubscriptionRefunds(Subscription subscription, Pageable pageable) {
        return refundRepository.findBySubscription(subscription, pageable);
    }

    /**
     * Get all pending approval refunds
     */
    public Page<SubscriptionRefund> getPendingApprovals(Pageable pageable) {
        return refundRepository.findPendingApprovals(pageable);
    }

    /**
     * Get all refunds with pagination
     */
    public Page<SubscriptionRefund> getAllRefunds(Pageable pageable) {
        return refundRepository.findAll(pageable);
    }

    /**
     * Get refunds by status
     */
    public Page<SubscriptionRefund> getRefundsByStatus(RefundStatus status, Pageable pageable) {
        return refundRepository.findByStatus(status, pageable);
    }

    /**
     * Count refunds pending approval
     */
    public long countPendingApprovals() {
        return refundRepository.countByStatus(RefundStatus.PENDING_APPROVAL);
    }

    /**
     * Approve a refund with fee deduction
     */
    public SubscriptionRefund approveRefund(Long refundId, BigDecimal processingFeePercent, String notes, User approvedByUser) {
        SubscriptionRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        if (refund.getStatus() != RefundStatus.REQUESTED && refund.getStatus() != RefundStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Refund cannot be approved in current status: " + refund.getStatus());
        }

        // Calculate processing fee
        BigDecimal processingFee = refund.getRequestedAmount()
                .multiply(processingFeePercent)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

        // Calculate approved amount (requested - fee)
        BigDecimal approvedAmount = refund.getRequestedAmount().subtract(processingFee);

        refund.setProcessingFee(processingFee);
        refund.setApprovedAmount(approvedAmount);
        refund.setStatus(RefundStatus.APPROVED);
        refund.setApprovedAt(LocalDateTime.now());
        refund.setApprovedByUser(approvedByUser);
        refund.setNotes(notes);

        SubscriptionRefund saved = refundRepository.save(refund);
        log.info("Refund {} approved with fee deduction. Approved amount: {}, Fee: {}", 
                 refundId, approvedAmount, processingFee);
        return saved;
    }

    /**
     * Approve a refund with default fee percentage
     */
    public SubscriptionRefund approveRefund(Long refundId, String notes, User approvedByUser) {
        return approveRefund(refundId, DEFAULT_PROCESSING_FEE_PERCENT, notes, approvedByUser);
    }

    /**
     * Reject a refund request
     */
    public SubscriptionRefund rejectRefund(Long refundId, String rejectionReason, User rejectedByUser) {
        SubscriptionRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        if (refund.getStatus() != RefundStatus.REQUESTED && refund.getStatus() != RefundStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Refund cannot be rejected in current status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.REJECTED);
        refund.setRejectionReason(rejectionReason);
        refund.setApprovedByUser(rejectedByUser);

        SubscriptionRefund saved = refundRepository.save(refund);
        log.info("Refund {} rejected. Reason: {}", refundId, rejectionReason);
        return saved;
    }

    /**
     * Mark refund as processing in Square
     */
    public SubscriptionRefund markAsProcessing(Long refundId) {
        SubscriptionRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        if (refund.getStatus() != RefundStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved refunds can be marked as processing");
        }

        refund.setStatus(RefundStatus.PROCESSING);
        SubscriptionRefund saved = refundRepository.save(refund);
        log.info("Refund {} marked as processing", refundId);
        return saved;
    }

    /**
     * Mark refund as completed with Square refund ID
     */
    public SubscriptionRefund completeRefund(Long refundId, String squareRefundId, BigDecimal refundedAmount) {
        SubscriptionRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        if (refund.getStatus() != RefundStatus.PROCESSING && refund.getStatus() != RefundStatus.APPROVED) {
            throw new IllegalArgumentException("Refund cannot be completed in current status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setSquareRefundId(squareRefundId);
        refund.setRefundedAmount(refundedAmount);
        refund.setCompletedAt(LocalDateTime.now());

        SubscriptionRefund saved = refundRepository.save(refund);
        log.info("Refund {} completed with Square refund ID: {}", refundId, squareRefundId);
        return saved;
    }

    /**
     * Mark refund as failed
     */
    public SubscriptionRefund failRefund(Long refundId, String errorReason) {
        SubscriptionRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        refund.setStatus(RefundStatus.FAILED);
        refund.setNotes("Error: " + errorReason);

        SubscriptionRefund saved = refundRepository.save(refund);
        log.warn("Refund {} failed. Reason: {}", refundId, errorReason);
        return saved;
    }

    /**
     * Get refunds approved but not yet processed in Square
     */
    public Page<SubscriptionRefund> getApprovedRefundsPendingSquareProcessing(Pageable pageable) {
        return refundRepository.findApprovedRefundsPendingSquareProcessing(pageable);
    }

    /**
     * Get refunds by date range
     */
    public List<SubscriptionRefund> getRefundsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return refundRepository.findRefundsByDateRange(startDate, endDate);
    }

    /**
     * Get total refunded amount for a subscription
     */
    public BigDecimal getTotalRefundedAmount(Subscription subscription) {
        return refundRepository.findBySubscription(subscription).stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                .map(SubscriptionRefund::getRefundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total fees deducted
     */
    public BigDecimal getTotalFeesDeducted(Subscription subscription) {
        return refundRepository.findBySubscription(subscription).stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED || r.getStatus() == RefundStatus.APPROVED)
                .map(SubscriptionRefund::getProcessingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Update refund notes
     */
    public SubscriptionRefund updateRefundNotes(Long refundId, String notes) {
        SubscriptionRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        refund.setNotes(notes);
        return refundRepository.save(refund);
    }
}
