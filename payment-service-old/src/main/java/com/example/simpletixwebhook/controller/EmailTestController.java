package com.example.simpletixwebhook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.simpletixwebhook.service.EmailService;

@RestController
public class EmailTestController {
    private final EmailService emailService;

    @Autowired
    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam String to) {
        emailService.sendSimpleEmail(to, "Test Email", "This is a test email from your Spring Boot app.");
        return ResponseEntity.ok("Email sent to " + to);
    }
}
