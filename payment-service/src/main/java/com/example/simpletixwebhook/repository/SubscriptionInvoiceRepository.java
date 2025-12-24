package com.example.simpletixwebhook.repository;

import com.example.simpletixwebhook.model.SubscriptionInvoice;
import com.example.simpletixwebhook.model.SubscriptionInvoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, Long> {
    List<SubscriptionInvoice> findBySubscriptionId(Long subscriptionId);
    List<SubscriptionInvoice> findByStatus(InvoiceStatus status);
}
