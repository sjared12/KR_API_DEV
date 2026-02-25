package com.krhscougarband.paymentportal.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }

    @GetMapping("/profile")
    public String profile() {
        // Redirect to dashboard where profile management is now centralized
        return "redirect:/payments/dashboard";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/user-plans")
    public String userPlans() {
        return "user-plans";
    }

    @GetMapping("/admin-plans")
    public String adminPlans() {
        return "admin-plans";
    }

    @GetMapping("/settings")
    public String settings() {
        // Redirect to dashboard where all user settings are now managed
        return "redirect:/payments/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
