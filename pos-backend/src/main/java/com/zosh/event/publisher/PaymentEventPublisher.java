package com.zosh.event.publisher;

import com.zosh.event.PaymentFailedEvent;
import com.zosh.event.PaymentInitiatedEvent;
import com.zosh.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publisher for payment-related domain events.
 * Uses Spring's ApplicationEventPublisher to broadcast events to listeners.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publish a payment initiated event.
     * All registered listeners will receive this event asynchronously.
     *
     * @param event The payment initiated event to publish
     */
    public void publishPaymentInitiated(PaymentInitiatedEvent event) {


        applicationEventPublisher.publishEvent(event);

        log.debug("PaymentInitiatedEvent published successfully for payment ID: {}",
            event.getPaymentId());
    }

    /**
     * Publish a payment success event.
     * All registered listeners will receive this event asynchronously.
     *
     * @param event The payment success event to publish
     */
    public void publishPaymentSuccess(PaymentSuccessEvent event) {


        applicationEventPublisher.publishEvent(event);

        log.debug("PaymentSuccessEvent published successfully for payment ID: {}",
            event.getPaymentId());
    }

    /**
     * Publish a payment failed event.
     * All registered listeners will receive this event asynchronously.
     *
     * @param event The payment failed event to publish
     */
    public void publishPaymentFailed(PaymentFailedEvent event) {


        applicationEventPublisher.publishEvent(event);

        log.debug("PaymentFailedEvent published successfully for payment ID: {}",
            event.getPaymentId());
    }
}
