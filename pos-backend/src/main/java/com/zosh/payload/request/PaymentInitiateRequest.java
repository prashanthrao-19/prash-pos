package com.zosh.payload.request;

import com.zosh.domain.PaymentGateway;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating a payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull(message = "Store ID is mandatory")
    private Long storeId;

    @NotNull(message = "subscriptionId is required")
    private Long subscriptionId; // Required only for FINE payments


    @NotNull(message = "Payment gateway is mandatory")
    private PaymentGateway gateway; // RAZORPAY or STRIPE

    @NotNull(message = "Amount is mandatory")
    @Positive(message = "Amount must be positive")
    private Double amount;

    private String description;


}
