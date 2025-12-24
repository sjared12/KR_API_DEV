package com.example.simpletixwebhook.controller.dto;

import java.math.BigDecimal;

/**
 * DTO for updating subscription amount
 */
public record UpdateSubscriptionAmountRequest(
    BigDecimal newAmount
) {}
