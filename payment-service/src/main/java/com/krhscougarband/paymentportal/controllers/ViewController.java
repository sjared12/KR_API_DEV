package com.krhscougarband.paymentportal.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/plans")
    public String plans() {
        return "user-plans";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin-plans";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}
