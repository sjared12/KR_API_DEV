package com.krhscougarband.krajdmin.repositories.payment;

import com.krhscougarband.krajdmin.entities.payment.PaymentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {
    Page<PaymentPlan> findByStudentId(String studentId, Pageable pageable);
    Page<PaymentPlan> findByStatus(PaymentPlan.Status status, Pageable pageable);
    Page<PaymentPlan> findByStudentIdAndStatus(String studentId, PaymentPlan.Status status, Pageable pageable);
    List<PaymentPlan> findByStudentIdAndStatus(String studentId, PaymentPlan.Status status);
    Optional<PaymentPlan> findByInvoiceId(String invoiceId);
}
