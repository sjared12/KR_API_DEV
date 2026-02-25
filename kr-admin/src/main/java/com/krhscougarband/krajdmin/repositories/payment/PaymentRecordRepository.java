package com.krhscougarband.krajdmin.repositories.payment;

import com.krhscougarband.krajdmin.entities.payment.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByPaymentPlanId(Long planId);
}
