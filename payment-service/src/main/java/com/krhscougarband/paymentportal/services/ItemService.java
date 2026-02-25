package com.krhscougarband.paymentportal.services;

import com.krhscougarband.paymentportal.entities.Item;
import com.krhscougarband.paymentportal.repositories.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<Item> getAvailableItems() {
        return itemRepository.findByAvailableTrue();
    }

    public List<Item> getItemsByCategory(String category) {
        return itemRepository.findByCategoryAndAvailableTrue(category);
    }

    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public Item createItem(Item item) {
        return itemRepository.save(item);
    }

    public Item updateItem(Item item) {
        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }
}
