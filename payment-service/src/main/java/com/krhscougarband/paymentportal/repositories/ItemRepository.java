package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByAvailableTrue();
    List<Item> findByCategoryAndAvailableTrue(String category);
    List<Item> findByCategory(String category);
}
