package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.PaymentLog;
import com.krhscougarband.paymentportal.entities.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, UUID> {

    List<PaymentLog> findByPlan(Plan plan);
}
