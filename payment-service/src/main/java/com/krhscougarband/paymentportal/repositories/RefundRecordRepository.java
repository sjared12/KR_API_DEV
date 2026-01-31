package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.Plan;
import com.krhscougarband.paymentportal.entities.RefundRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecord, UUID> {
    List<RefundRecord> findByPlan(Plan plan);
    
    Page<RefundRecord> findByPlan(Plan plan, Pageable pageable);
    
    Page<RefundRecord> findByStatus(String status, Pageable pageable);
    
    List<RefundRecord> findByPlanAndStatus(Plan plan, String status);
}
