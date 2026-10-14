package com.zosh.event;

import com.zosh.domain.PaymentGateway;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event published when a payment is initiated.
 * This event can be used for tracking, analytics, and notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiatedEvent {

    /**
     * ID of the payment that was initiated
     */
    private Long paymentId;

    /**
     * ID of the user who initiated the payment
     */
    private Long storeId;


    /**
     * Payment gateway being used
     */
    private PaymentGateway provider;

    /**
     * Amount to be paid
     */
    private Double amount;


    /**
     * ID of related book loan (if applicable)
     */
    private Long subscriptionId;

    /**
     * Transaction ID
     */
    private String transactionId;

    /**
     * Timestamp when payment was initiated
     */
    private LocalDateTime initiatedAt;

    /**
     * Description of the payment
     */
    private String description;

    /**
     * Checkout URL for user to complete payment
     */
    private String checkoutUrl;


    private String storeName;
}
