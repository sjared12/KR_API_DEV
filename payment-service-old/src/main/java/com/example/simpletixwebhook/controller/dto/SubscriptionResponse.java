package com.example.simpletixwebhook.controller.dto;

import com.example.simpletixwebhook.model.Subscription.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for subscription response
 */
public record SubscriptionResponse(
    Long id,
    String squareSubscriptionId,
    String squareCustomerId,
    String customerEmail,
    String customerName,
    BigDecimal amount,
    BigDecimal amountPaid,
    SubscriptionStatus status,
    String currency,
    String description,
    String billingCycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime canceledAt,
    String cancellationReason,
    BigDecimal amountPerPayment,
    String frequency,
    LocalDateTime estimatedEndDate,
    Integer numberOfPayments
) {}
