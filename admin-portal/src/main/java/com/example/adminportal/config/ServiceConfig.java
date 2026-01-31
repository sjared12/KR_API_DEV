package com.example.adminportal.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Service configuration that is persisted to the config file.
 * Each service (api, payment, feedback, etc.) can have its own config section.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceConfig {
    private String service;
    private Object properties;
}
