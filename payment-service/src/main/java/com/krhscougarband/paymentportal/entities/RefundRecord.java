package com.krhscougarband.paymentportal.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refund_records")
@Data
@NoArgsConstructor
public class RefundRecord {
        // Lombok @Data provides getters/setters, but add explicit methods for clarity/framework compatibility
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public Plan getPlan() { return plan; }
        public void setPlan(Plan plan) { this.plan = plan; }
        public BigDecimal getRefundAmount() { return refundAmount; }
        public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
        public String getRefundReason() { return refundReason; }
        public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSquareRefundId() { return squareRefundId; }
        public void setSquareRefundId(String squareRefundId) { this.squareRefundId = squareRefundId; }
        public String getRefundMethod() { return refundMethod; }
        public void setRefundMethod(String refundMethod) { this.refundMethod = refundMethod; }
        public User getInitiatedByUser() { return initiatedByUser; }
        public void setInitiatedByUser(User initiatedByUser) { this.initiatedByUser = initiatedByUser; }
        public User getApprovedByUser() { return approvedByUser; }
        public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
        public LocalDateTime getApprovedAt() { return approvedAt; }
        public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    private BigDecimal refundAmount;
    
    private String refundReason;
    
    private String status; // REQUESTED, APPROVED, PROCESSED, FAILED
    
    private String squareRefundId; // Square refund transaction ID
    
    private String refundMethod; // CARD, MANUAL, OTHER
    
    @ManyToOne
    @JoinColumn(name = "initiated_by_user_id")
    private User initiatedByUser;
    
    @ManyToOne
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;
    
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    private LocalDateTime approvedAt;
    
    private LocalDateTime processedAt;
    
    private String notes;

    @PrePersist
    public void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "REQUESTED";
        }
    }
}
