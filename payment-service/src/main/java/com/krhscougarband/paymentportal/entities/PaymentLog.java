package com.krhscougarband.paymentportal.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_logs")
@Data
public class PaymentLog {
    // Lombok @Data provides getters/setters, but add explicit methods for clarity/framework compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private Plan plan;

    private BigDecimal amount;

    private String status; // PAID, FAILED, REFUNDED
    
    private String paymentMethod; // CARD, CASH, OTHER
    
    private String transactionId; // Square payment ID

    private LocalDateTime timestamp = LocalDateTime.now();
}
