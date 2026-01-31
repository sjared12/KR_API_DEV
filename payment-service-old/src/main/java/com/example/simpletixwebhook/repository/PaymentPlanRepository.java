package com.example.simpletixwebhook.repository;

import com.example.simpletixwebhook.model.PaymentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {
    java.util.Optional<PaymentPlan> findByInvoiceId(String invoiceId);

    Page<PaymentPlan> findAll(@NonNull Pageable pageable);
    Page<PaymentPlan> findByStatus(PaymentPlan.Status status, Pageable pageable);
    Page<PaymentPlan> findByStudentId(String studentId, Pageable pageable);
    Page<PaymentPlan> findByStudentIdAndStatus(String studentId, PaymentPlan.Status status, Pageable pageable);
}
