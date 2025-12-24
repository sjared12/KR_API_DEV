package com.example.adminservice.repository;

import com.example.adminservice.model.ManagedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagedServiceRepository extends JpaRepository<ManagedService, Long> {
    Optional<ManagedService> findByServiceName(String serviceName);
}
