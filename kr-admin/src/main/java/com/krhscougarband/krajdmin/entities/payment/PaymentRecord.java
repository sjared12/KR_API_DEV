package com.krhscougarband.krajdmin.entities.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment record for tracking transactions
 */
@Entity
@Table(name = "payment_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PaymentPlan paymentPlan;

    @Column(nullable = false)
    private BigDecimal amount;

    private String squarePaymentId;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
