package com.example.adminservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "managed_services")
public class ManagedService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Column(nullable = false)
    private String serviceUrl;

    @Column(nullable = false)
    private String servicePort;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean healthy = false;

    @Column(name = "health_check_url")
    private String healthCheckUrl;

    @Column(name = "logs_url")
    private String logsUrl;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public ManagedService() {}

    public ManagedService(String serviceName, String serviceUrl, String servicePort, String description) {
        this.serviceName = serviceName;
        this.serviceUrl = serviceUrl;
        this.servicePort = servicePort;
        this.description = description;
        this.enabled = true;
        this.healthCheckUrl = serviceUrl + ":" + servicePort + "/api/health";
        this.logsUrl = serviceUrl + ":" + servicePort + "/api/logs";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }

    public String getServicePort() { return servicePort; }
    public void setServicePort(String servicePort) { this.servicePort = servicePort; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Boolean getHealthy() { return healthy; }
    public void setHealthy(Boolean healthy) { this.healthy = healthy; }

    public String getHealthCheckUrl() { return healthCheckUrl; }
    public void setHealthCheckUrl(String healthCheckUrl) { this.healthCheckUrl = healthCheckUrl; }

    public String getLogsUrl() { return logsUrl; }
    public void setLogsUrl(String logsUrl) { this.logsUrl = logsUrl; }

    public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
    public void setLastHealthCheck(LocalDateTime lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFullUrl() {
        return serviceUrl + ":" + servicePort;
    }
}
