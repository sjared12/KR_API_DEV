package com.example.simpletixwebhook.controller.dto;

import java.math.BigDecimal;

/**
 * DTO for approving a refund
 */
public record ApproveRefundRequest(
    BigDecimal processingFeePercent,
    String notes
) {}
