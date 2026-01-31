package com.krhscougarband.paymentportal.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundDto {
    private BigDecimal amount;
}
