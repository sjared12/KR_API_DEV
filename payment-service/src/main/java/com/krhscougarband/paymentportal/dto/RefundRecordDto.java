package com.krhscougarband.paymentportal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundRecordDto {
    private String id;
    private String planId;
    private BigDecimal refundAmount;
    private String refundReason;
    private String status;
    private String squareRefundId;
    private String refundMethod;
    private String initiatedByUserEmail;
    private String approvedByUserEmail;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;
    private String notes;
}
