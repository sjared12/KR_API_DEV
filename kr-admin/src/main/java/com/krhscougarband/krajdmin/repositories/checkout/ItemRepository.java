package com.krhscougarband.krajdmin.repositories.checkout;

import com.krhscougarband.krajdmin.entities.checkout.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByCategory(String category);
    List<Item> findByAvailable(Boolean available);
}
