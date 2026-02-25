package com.krhscougarband.paymentportal.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Data
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private User owner;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String name;
    private BigDecimal amount;
    private String frequency; // WEEKLY, MONTHLY
    private String currency;
    private String squareSubscriptionId;
    private BigDecimal totalOwed;
    private BigDecimal amountPaid;
    private String status; // ACTIVE, CANCELLED, ERROR, COMPLETED
    private boolean active;
    private boolean paused; // Whether subscription is paused
    
    // Credit card information
    private String cardLast4;
    private String cardBrand;
    private String cardHolderName;
    
    private LocalDateTime startDate;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefundRecord> refundRecords;

    // Lombok @Data provides getters/setters, but add explicit methods for clarity/framework compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSquareSubscriptionId() { return squareSubscriptionId; }
    public void setSquareSubscriptionId(String squareSubscriptionId) { this.squareSubscriptionId = squareSubscriptionId; }
    public BigDecimal getTotalOwed() { return totalOwed; }
    public void setTotalOwed(BigDecimal totalOwed) { this.totalOwed = totalOwed; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<RefundRecord> getRefundRecords() { return refundRecords; }
    public void setRefundRecords(List<RefundRecord> refundRecords) { this.refundRecords = refundRecords; }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (startDate == null) {
            startDate = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO;
        }
        if (status == null) {
            status = "ACTIVE";
        }
        active = true;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate the next charge date based on startDate and frequency
     * Returns null for ONE_TIME or CANCELLED plans
     */
    @Transient
    public LocalDateTime calculateNextChargeDate() {
        if (startDate == null || "CANCELLED".equals(status) || "ONE_TIME".equals(frequency)) {
            return null;
        }
        
        LocalDateTime nextCharge = startDate;
        
        // If we have a lastChargedDate, calculate from there
        // For now, assume we charge from startDate + frequency interval
        switch (frequency) {
            case "MONTHLY":
                nextCharge = startDate.plusMonths(1);
                break;
            case "QUARTERLY":
                nextCharge = startDate.plusMonths(3);
                break;
            case "SEMI_ANNUAL":
                nextCharge = startDate.plusMonths(6);
                break;
            case "ANNUAL":
                nextCharge = startDate.plusYears(1);
                break;
            case "ONE_TIME":
                return null;
            default:
                nextCharge = startDate.plusMonths(1); // Default to monthly
        }
        
        return nextCharge;
    }
}
