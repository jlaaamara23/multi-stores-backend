package com.example.multi_stores.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentIntentRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String currency;

    private Long orderId;

    public PaymentIntentRequest() {}

    public PaymentIntentRequest(BigDecimal amount, String currency, Long orderId) {
        this.amount = amount;
        this.currency = currency;
        this.orderId = orderId;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}
