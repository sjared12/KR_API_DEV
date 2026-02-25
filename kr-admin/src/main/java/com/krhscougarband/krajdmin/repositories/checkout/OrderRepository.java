package com.krhscougarband.krajdmin.repositories.checkout;

import com.krhscougarband.krajdmin.entities.checkout.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserEmail(String userEmail, Pageable pageable);
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
}
