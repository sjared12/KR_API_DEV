package com.example.simpletixwebhook.controller.dto;

/**
 * DTO for rejecting a refund
 */
public record RejectRefundRequest(
    String rejectionReason
) {}
