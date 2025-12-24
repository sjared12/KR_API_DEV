package com.example.simpletixwebhook.controller.dto;

import java.math.BigDecimal;

/**
 * DTO for marking refund as completed in Square
 */
public record CompleteRefundRequest(
    String squareRefundId,
    BigDecimal refundedAmount
) {}
