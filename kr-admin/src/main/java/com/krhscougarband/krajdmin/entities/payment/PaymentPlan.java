package com.krhscougarband.krajdmin.entities.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment plan for recurring payments from students/families
 */
@Entity
@Table(name = "payment_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private BigDecimal totalDue;

    @Column(nullable = false)
    private BigDecimal installmentAmount;

    @Column(nullable = false)
    private BigDecimal remaining;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cadence cadence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    private String invoiceId;

    private String squareCustomerId;

    private String squareCardId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        ACTIVE, COMPLETE, CANCELED, PAUSED
    }

    public enum Cadence {
        WEEKLY, BIWEEKLY, MONTHLY
    }
}
