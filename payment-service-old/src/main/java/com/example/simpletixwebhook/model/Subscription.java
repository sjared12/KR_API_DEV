package com.example.simpletixwebhook.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Subscription entity representing a Square subscription managed by the system.
 * Tracks subscription lifecycle, payment amounts, and integration with Square.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {
    public enum SubscriptionStatus {
        PENDING,      // Subscription created but not yet active in Square
        ACTIVE,       // Active subscription in Square
        PAUSED,       // Paused subscription
        CANCELED,     // Canceled subscription
        EXPIRED,      // Subscription expired
        ERROR         // Error state
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String squareSubscriptionId;

    @Column(nullable = false, length = 100)
    private String squareCustomerId;

    @Column(nullable = false, length = 100)
    private String squarePlanId;

    @Column(nullable = false, length = 255)
    private String customerEmail;

    @Column(length = 255)
    private String customerName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(length = 50)
    private String currency = "USD";

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String cancellationReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime canceledAt;

    @Column(length = 100)
    private String billingCycle = "MONTHLY";

    @Column(precision = 10, scale = 2)
    private BigDecimal amountPerPayment;

    @Column(length = 100)
    private String frequency;

    @Column
    private LocalDateTime estimatedEndDate;

    @Column
    private Integer numberOfPayments;

    public Subscription() {
    }

    public Subscription(String squareSubscriptionId, String squareCustomerId, String squarePlanId,
                       String customerEmail, BigDecimal amount) {
        this.squareSubscriptionId = squareSubscriptionId;
        this.squareCustomerId = squareCustomerId;
        this.squarePlanId = squarePlanId;
        this.customerEmail = customerEmail;
        this.amount = amount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSquareSubscriptionId() {
        return squareSubscriptionId;
    }

    public void setSquareSubscriptionId(String squareSubscriptionId) {
        this.squareSubscriptionId = squareSubscriptionId;
    }

    public String getSquareCustomerId() {
        return squareCustomerId;
    }

    public void setSquareCustomerId(String squareCustomerId) {
        this.squareCustomerId = squareCustomerId;
    }

    public String getSquarePlanId() {
        return squarePlanId;
    }

    public void setSquarePlanId(String squarePlanId) {
        this.squarePlanId = squarePlanId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public BigDecimal getAmountPerPayment() {
        return amountPerPayment;
    }

    public void setAmountPerPayment(BigDecimal amountPerPayment) {
        this.amountPerPayment = amountPerPayment;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public LocalDateTime getEstimatedEndDate() {
        return estimatedEndDate;
    }

    public void setEstimatedEndDate(LocalDateTime estimatedEndDate) {
        this.estimatedEndDate = estimatedEndDate;
    }

    public Integer getNumberOfPayments() {
        return numberOfPayments;
    }

    public void setNumberOfPayments(Integer numberOfPayments) {
        this.numberOfPayments = numberOfPayments;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
