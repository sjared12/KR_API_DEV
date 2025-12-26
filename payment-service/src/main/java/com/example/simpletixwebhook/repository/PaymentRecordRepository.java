package com.example.simpletixwebhook.repository;

import com.example.simpletixwebhook.model.PaymentRecord;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    boolean existsBySquarePaymentId(String squarePaymentId);
    Optional<PaymentRecord> findTopByOrderByCreatedAtDesc();
    Page<PaymentRecord> findByStudentId(String studentId, Pageable pageable);
    Page<PaymentRecord> findAll(Pageable pageable);
    Page<PaymentRecord> findByCreatedAtAfter(Instant instant, Pageable pageable);
}
