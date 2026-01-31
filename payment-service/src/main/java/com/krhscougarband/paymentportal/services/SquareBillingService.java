package com.krhscougarband.paymentportal.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@Service
public class SquareBillingService {

    private static final Logger log = LoggerFactory.getLogger(SquareBillingService.class);

    @Value("${square.access-token:}")
    private String accessToken;

    @Value("${square.environment:SANDBOX}")
    private String environment;

    public void createSubscription(String customerId, String planId, BigDecimal amount) {
        // Stubbed no-op until Square integration is wired.
        // We log intent so later we can replay/implement real billing safely.
        log.info("[Square stub] createSubscription customerId={} planId={} amount={} env={} tokenPresent={}",
                customerId, planId, amount, environment, accessToken != null && !accessToken.isBlank());
    }

    public void updateSubscription(String subscriptionId, BigDecimal newAmount, String frequency) {
        log.info("[Square stub] updateSubscription subscriptionId={} newAmount={} frequency={} env={} tokenPresent={}",
                subscriptionId, newAmount, frequency, environment, accessToken != null && !accessToken.isBlank());
    }

    public void refundPayment(String paymentId, BigDecimal amount) {
        log.info("[Square stub] refundPayment paymentId={} amount={} env={} tokenPresent={}",
                paymentId, amount, environment, accessToken != null && !accessToken.isBlank());
    }
}
