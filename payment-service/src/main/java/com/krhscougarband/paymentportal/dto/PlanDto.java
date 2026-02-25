package com.krhscougarband.paymentportal.dto;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

@Data
public class PlanDto {
        // Lombok @Data provides getters/setters, but add explicit methods for clarity/framework compatibility
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    private String id;
    private String name;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @DecimalMin(value = "0.01", message = "Total owed must be greater than 0")
    private BigDecimal totalOwed;
    
    private BigDecimal amountPaid;
    
    @NotBlank(message = "Frequency is required")
    private String frequency;
    private String currency;
    private String status;
    private boolean paused;
    private String startDate;
    private String nextChargeDate;
    private String studentId;
    private String studentName;
    private String userEmail;
}
