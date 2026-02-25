package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.InstrumentItemRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstrumentItemRuleRepository extends JpaRepository<InstrumentItemRule, Long> {
    List<InstrumentItemRule> findByInstrumentIgnoreCase(String instrument);
}
