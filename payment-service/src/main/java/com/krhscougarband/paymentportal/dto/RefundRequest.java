package com.krhscougarband.paymentportal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal refundAmount;
    
    @NotBlank(message = "Refund reason is required")
    private String refundReason;
    
    private String refundMethod; // CARD, MANUAL, OTHER
    
    private String notes;
}
