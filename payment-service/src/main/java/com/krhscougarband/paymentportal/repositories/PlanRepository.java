package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.Plan;
import com.krhscougarband.paymentportal.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    // Get all plans for a specific user
    List<Plan> findByOwner(User owner);
}
