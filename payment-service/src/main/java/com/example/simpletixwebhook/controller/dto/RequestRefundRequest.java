package com.example.simpletixwebhook.controller.dto;

import java.math.BigDecimal;

/**
 * DTO for requesting a refund
 */
public record RequestRefundRequest(
    Long subscriptionId,
    BigDecimal requestedAmount,
    String reason
) {}
