package com.krhscougarband.paymentportal.services;

import com.krhscougarband.paymentportal.dtos.CreateOrderRequest;
import com.krhscougarband.paymentportal.dtos.OrderItemRequest;
import com.krhscougarband.paymentportal.entities.*;
import com.krhscougarband.paymentportal.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final InstrumentItemRuleRepository instrumentItemRuleRepository;
    private final UserProfileRepository userProfileRepository;

    public OrderService(OrderRepository orderRepository,
                        ItemRepository itemRepository,
                        InstrumentItemRuleRepository instrumentItemRuleRepository,
                        UserProfileRepository userProfileRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.instrumentItemRuleRepository = instrumentItemRuleRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public Order createOrder(String userId, String userEmail, CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(userId);
        order.setUserEmail(userEmail);
        order.setNotes(request.getNotes());

        Map<Long, Integer> mergedQuantities = new HashMap<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
            mergedQuantities.merge(itemRequest.getItemId(), itemRequest.getQuantity(), Integer::sum);
        }

        UserProfile profile = userProfileRepository.findByUserEmail(userEmail).orElse(null);
        if (profile != null && profile.getInstrument() != null && !profile.getInstrument().isBlank()) {
            List<InstrumentItemRule> rules = instrumentItemRuleRepository
                .findByInstrumentIgnoreCase(profile.getInstrument().trim());
            for (InstrumentItemRule rule : rules) {
                mergedQuantities.merge(rule.getItem().getId(), rule.getQuantity(), Integer::sum);
            }
        }

        for (Map.Entry<Long, Integer> entry : mergedQuantities.entrySet()) {
            Item item = itemRepository.findById(entry.getKey())
                .orElseThrow(() -> new RuntimeException("Item not found: " + entry.getKey()));

            if (!item.getAvailable()) {
                throw new RuntimeException("Item not available: " + item.getName());
            }

            if (item.getStockQuantity() != null && item.getStockQuantity() < entry.getValue()) {
                throw new RuntimeException("Insufficient stock for: " + item.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setItem(item);
            orderItem.setQuantity(entry.getValue());
            orderItem.setUnitPrice(item.getPrice());
            order.addItem(orderItem);

            if (item.getStockQuantity() != null) {
                item.setStockQuantity(item.getStockQuantity() - entry.getValue());
                itemRepository.save(item);
            }
        }

        return orderRepository.save(order);
    }

    public List<Order> getOrdersForUser(String userEmail) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }

    @Transactional
    public void cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        
        // Can only cancel pending or confirmed orders
        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }
        
        // Restore stock for all items in the order
        for (OrderItem item : order.getItems()) {
            Item itemEntity = itemRepository.findById(item.getItem().getId())
                .orElse(null);
            if (itemEntity != null && itemEntity.getStockQuantity() != null) {
                itemEntity.setStockQuantity(itemEntity.getStockQuantity() + item.getQuantity());
                itemRepository.save(itemEntity);
            }
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}