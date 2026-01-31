package com.krhscougarband.paymentportal.services;

import com.krhscougarband.paymentportal.entities.PaymentLog;
import com.krhscougarband.paymentportal.entities.Plan;
import com.krhscougarband.paymentportal.repositories.PaymentLogRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuditService {

    private final PaymentLogRepository paymentLogRepository;

    public AuditService(PaymentLogRepository paymentLogRepository) {
        this.paymentLogRepository = paymentLogRepository;
    }

    public void logPayment(Plan plan, BigDecimal amount, String status, String transactionId) {
        PaymentLog log = new PaymentLog();
        log.setPlan(plan);
        log.setAmount(amount);
        log.setStatus(status);
        log.setTransactionId(transactionId);

        paymentLogRepository.save(log);
    }
}
