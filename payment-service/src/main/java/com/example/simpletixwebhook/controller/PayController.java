package com.example.simpletixwebhook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PayController {

    @GetMapping({"/pay", "/pay/"})
    public String payRoot() {
        return "forward:/pay.html";
    }

    @GetMapping({"/pay/{studentId}", "/pay/{studentId}/{due}"})
    public String payWithParams() {
        return "forward:/pay.html";
    }

    @GetMapping("/pay-error")
    public String payError() {
        return "forward:/pay-error.html";
    }

    // Subscription Management GUI Routes
    @GetMapping({"/subscription-manager", "/dashboards/subscriptions"})
    public String subscriptionManager() {
        return "forward:/subscription-manager.html";
    }
}
