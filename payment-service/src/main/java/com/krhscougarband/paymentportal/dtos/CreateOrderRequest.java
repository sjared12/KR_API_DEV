package com.krhscougarband.paymentportal.dtos;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateOrderRequest {
    @NotEmpty
    private List<OrderItemRequest> items;

    private String notes;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
