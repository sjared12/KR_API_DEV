package com.krhscougarband.paymentportal.dto;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;

@Data
public class PlanUpdateDto {
        // Lombok @Data provides getters/setters, but add explicit methods for clarity/framework compatibility
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }
    private String name;
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    private BigDecimal totalOwed;
    private String frequency;
    private String currency;
    private String cardId; // optional

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTotalOwed() { return totalOwed; }
    public void setTotalOwed(BigDecimal totalOwed) { this.totalOwed = totalOwed; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
