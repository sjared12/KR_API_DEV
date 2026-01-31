package com.example.simpletixwebhook.controller.dto;

import com.example.simpletixwebhook.model.SubscriptionRefund.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for refund response
 */
public record RefundResponse(
    Long id,
    Long subscriptionId,
    String squareSubscriptionId,
    BigDecimal requestedAmount,
    BigDecimal processingFee,
    BigDecimal approvedAmount,
    BigDecimal refundedAmount,
    RefundStatus status,
    String refundReason,
    String rejectionReason,
    String squareRefundId,
    String requestedByUser,
    String approvedByUser,
    LocalDateTime createdAt,
    LocalDateTime approvedAt,
    LocalDateTime completedAt,
    String notes
) {}
