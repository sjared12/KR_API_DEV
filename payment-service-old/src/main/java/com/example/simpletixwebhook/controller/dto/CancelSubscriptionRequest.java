package com.example.simpletixwebhook.controller.dto;

/**
 * DTO for canceling a subscription
 */
public record CancelSubscriptionRequest(
    String reason
) {}
