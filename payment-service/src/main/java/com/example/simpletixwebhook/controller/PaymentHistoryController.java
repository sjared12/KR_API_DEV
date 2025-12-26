package com.example.simpletixwebhook.controller;

import com.example.simpletixwebhook.model.PaymentRecord;
import com.example.simpletixwebhook.repository.PaymentRecordRepository;
import com.example.simpletixwebhook.service.SquarePaymentSyncService;
import com.example.simpletixwebhook.service.SquarePaymentSyncService.SyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentHistoryController {

    private static final Logger log = LoggerFactory.getLogger(PaymentHistoryController.class);

    private final PaymentRecordRepository paymentRecordRepository;
    private final SquarePaymentSyncService paymentSyncService;

    public PaymentHistoryController(PaymentRecordRepository paymentRecordRepository,
                                    SquarePaymentSyncService paymentSyncService) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentSyncService = paymentSyncService;
    }

    @PostMapping("/api/payments/sync")
    public ResponseEntity<SyncResult> sync() {
        try {
            SyncResult result = paymentSyncService.syncPayments();
            return ResponseEntity.ok(result);
        } catch (IllegalStateException ex) {
            log.error("Square sync not configured: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new SyncResult(0));
        } catch (Exception ex) {
            log.error("Failed to sync Square payments", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new SyncResult(0));
        }
    }

    @GetMapping("/api/payments/history")
    public Page<PaymentRecord> history(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "25") int size,
                                       @RequestParam(required = false) String studentId) {
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(200, size));
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (studentId != null && !studentId.isBlank()) {
            return paymentRecordRepository.findByStudentId(studentId, pageable);
        }
        return paymentRecordRepository.findAll(pageable);
    }
}
