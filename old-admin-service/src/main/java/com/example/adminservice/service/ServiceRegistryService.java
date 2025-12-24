package com.example.adminservice.service;

import com.example.adminservice.model.ManagedService;
import com.example.adminservice.repository.ManagedServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServiceRegistryService {
    @Autowired
    private ManagedServiceRepository managedServiceRepository;

    @Autowired
    private RestTemplate restTemplate;

    public ManagedService registerService(String serviceName, String serviceUrl, String servicePort, String description) {
        ManagedService service = new ManagedService(serviceName, serviceUrl, servicePort, description);
        return managedServiceRepository.save(service);
    }

    public List<ManagedService> getAllServices() {
        return managedServiceRepository.findAll();
    }

    public Optional<ManagedService> getServiceByName(String serviceName) {
        return managedServiceRepository.findByServiceName(serviceName);
    }

    public ManagedService updateService(Long id, String serviceName, String serviceUrl, String servicePort, String description) {
        ManagedService service = managedServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        service.setServiceName(serviceName);
        service.setServiceUrl(serviceUrl);
        service.setServicePort(servicePort);
        service.setDescription(description);
        service.setHealthCheckUrl(serviceUrl + ":" + servicePort + "/api/health");
        return managedServiceRepository.save(service);
    }

    public void deleteService(Long id) {
        managedServiceRepository.deleteById(id);
    }

    public boolean checkServiceHealth(Long serviceId) {
        ManagedService service = managedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    service.getHealthCheckUrl(), 
                    Map.class
            );
            boolean healthy = response.getStatusCode().is2xxSuccessful();
            service.setHealthy(healthy);
            service.setLastHealthCheck(LocalDateTime.now());
            managedServiceRepository.save(service);
            return healthy;
        } catch (Exception e) {
            service.setHealthy(false);
            service.setLastHealthCheck(LocalDateTime.now());
            managedServiceRepository.save(service);
            return false;
        }
    }

    public Map<String, Object> getServiceLogs(Long serviceId, int lines) {
        ManagedService service = managedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        
        try {
            String url = service.getFullUrl() + "/api/logs?lines=" + lines;
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch logs: " + e.getMessage());
        }
    }

    public Map<String, Object> getServiceMetrics(Long serviceId) {
        ManagedService service = managedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        
        try {
            String url = service.getFullUrl() + "/api/metrics";
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch metrics: " + e.getMessage());
        }
    }

    public void enableService(Long serviceId) {
        ManagedService service = managedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        service.setEnabled(true);
        managedServiceRepository.save(service);
    }

    public void disableService(Long serviceId) {
        ManagedService service = managedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        service.setEnabled(false);
        managedServiceRepository.save(service);
    }
}
