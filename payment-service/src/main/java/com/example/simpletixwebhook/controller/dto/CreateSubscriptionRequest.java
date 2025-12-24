package com.example.simpletixwebhook.controller.dto;

import java.math.BigDecimal;

/**
 * DTO for creating a subscription
 */
public record CreateSubscriptionRequest(
    String squareSubscriptionId,
    String squareCustomerId,
    String squarePlanId,
    String customerEmail,
    BigDecimal amount,
    String currency,
    String description,
    String billingCycle,
    BigDecimal amountPerPayment,
    String frequency,
    String estimatedEndDate,
    Integer numberOfPayments
) {}
