package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.entities.InstrumentItemRule;
import com.krhscougarband.paymentportal.repositories.InstrumentItemRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instrument-rules")
public class InstrumentRuleController {
    private final InstrumentItemRuleRepository instrumentItemRuleRepository;

    public InstrumentRuleController(InstrumentItemRuleRepository instrumentItemRuleRepository) {
        this.instrumentItemRuleRepository = instrumentItemRuleRepository;
    }

    @GetMapping("/{instrument}")
    public ResponseEntity<List<InstrumentItemRule>> getRules(@PathVariable String instrument) {
        return ResponseEntity.ok(instrumentItemRuleRepository.findByInstrumentIgnoreCase(instrument));
    }

    @PostMapping
    public ResponseEntity<InstrumentItemRule> createRule(@RequestBody InstrumentItemRule rule) {
        return ResponseEntity.ok(instrumentItemRuleRepository.save(rule));
    }
}
