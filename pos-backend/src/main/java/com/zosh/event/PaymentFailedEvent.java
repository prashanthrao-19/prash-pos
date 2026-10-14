package com.zosh.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event published when a payment fails.
 * This event allows services to react to payment failures
 * (e.g., notify users, log failures, trigger compensation logic).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {

    /**
     * ID of the payment that failed
     */
    private Long paymentId;

    /**
     * ID of the user who attempted the payment
     */
    private Long storeId;


    /**
     * Amount attempted
     */
    private Double amount;


    /**
     * ID of related book loan (if applicable)
     */
    private Long subscriptionId;

    /**
     * Reason for failure
     */
    private String failureReason;

    /**
     * Gateway payment ID for reference (if available)
     */
    private String providerPaymentId;

    /**
     * Transaction ID
     */
    private String transactionId;

    /**
     * Timestamp when payment failed
     */
    private LocalDateTime failedAt;

    /**
     * Description of the payment
     */
    private String description;


}
