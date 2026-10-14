package com.zosh.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event published when a payment is successfully completed.
 * This event decouples PaymentService from other domain services,
 * allowing them to react to payment success independently.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {

    /**
     * ID of the payment that succeeded
     */
    private Long paymentId;

    /**
     * ID of the user who made the payment
     */
    private Long storeId;


    /**
     * Amount paid
     */
    private Double amount;



    /**
     * ID of related book loan (if applicable)
     */
    private Long subscriptionId;

    /**
     * Gateway payment ID for reference
     */
    private String providerPaymentId;

    /**
     * Transaction ID
     */
    private String transactionId;

    /**
     * Timestamp when payment was completed
     */
    private LocalDateTime paidAt;

    /**
     * Description of the payment
     */
    private String description;
}
