package com.example.simpletixwebhook.repository;

import com.example.simpletixwebhook.model.Subscription;
import com.example.simpletixwebhook.model.SubscriptionRefund;
import com.example.simpletixwebhook.model.SubscriptionRefund.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SubscriptionRefund entity with custom queries for refund management.
 */
@Repository
public interface SubscriptionRefundRepository extends JpaRepository<SubscriptionRefund, Long> {
    /**
     * Find refund by Square refund ID
     */
    Optional<SubscriptionRefund> findBySquareRefundId(String squareRefundId);

    /**
     * Find all refunds for a subscription
     */
    List<SubscriptionRefund> findBySubscription(Subscription subscription);

    /**
     * Find all refunds for a subscription with pagination
     */
    Page<SubscriptionRefund> findBySubscription(Subscription subscription, Pageable pageable);

    /**
     * Find refunds by status
     */
    List<SubscriptionRefund> findByStatus(RefundStatus status);

    /**
     * Find refunds by status with pagination
     */
    Page<SubscriptionRefund> findByStatus(RefundStatus status, Pageable pageable);

    /**
     * Find pending approval refunds (sorted by created date)
     */
    @Query("SELECT r FROM SubscriptionRefund r WHERE r.status = 'PENDING_APPROVAL' ORDER BY r.createdAt ASC")
    Page<SubscriptionRefund> findPendingApprovals(Pageable pageable);

    /**
     * Find refunds created within a date range
     */
    @Query("SELECT r FROM SubscriptionRefund r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<SubscriptionRefund> findRefundsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find refunds by subscription and status
     */
    List<SubscriptionRefund> findBySubscriptionAndStatus(Subscription subscription, RefundStatus status);

    /**
     * Count pending approvals
     */
    long countByStatus(RefundStatus status);

    /**
     * Find approved refunds ready for processing
     */
    @Query("SELECT r FROM SubscriptionRefund r WHERE r.status = 'APPROVED' AND r.squareRefundId IS NULL ORDER BY r.approvedAt ASC")
    Page<SubscriptionRefund> findApprovedRefundsPendingSquareProcessing(Pageable pageable);
}
