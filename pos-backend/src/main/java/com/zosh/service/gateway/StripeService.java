package com.zosh.service.gateway;

import com.zosh.exception.PaymentException;
import com.zosh.modal.Payment;
import com.zosh.modal.Subscription;
import com.zosh.modal.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for Stripe payment gateway integration
 * Handles payment intent creation and verification
 */
@Service
@Slf4j
public class StripeService {

    @Value("${stripe.api.key:}")
    private String stripeSecretKey;

    @Value("${stripe.publishable.key:}")
    private String stripePublishableKey;


    /**
     * Verify Stripe payment
     */
    public boolean verifyPayment(String paymentIntentId) throws PaymentException {
        try {
            log.info("Verifying Stripe payment: {}", paymentIntentId);

            // In production, use Stripe SDK:
            // Stripe.apiKey = stripeSecretKey;
            // PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            // return "succeeded".equals(paymentIntent.getStatus());

            // For demo purposes
            log.info("Stripe payment verification successful");
            return true;

        } catch (Exception e) {
            log.error("Error verifying Stripe payment", e);
            throw new PaymentException("Failed to verify Stripe payment: " + e.getMessage());
        }
    }

    /**
     * Retrieve payment intent details
     */
    public String getPaymentIntentStatus(String paymentIntentId) throws PaymentException {
        try {
            log.info("Retrieving Stripe PaymentIntent status: {}", paymentIntentId);

            // In production, use Stripe SDK:
            // Stripe.apiKey = stripeSecretKey;
            // PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            // return paymentIntent.getStatus();

            // For demo purposes
            return "succeeded";

        } catch (Exception e) {
            log.error("Error retrieving Stripe PaymentIntent status", e);
            throw new PaymentException("Failed to retrieve payment status: " + e.getMessage());
        }
    }

    /**
     * Create subscription payment link for Stripe
     */
    public String createSubscriptionPaymentLink(User user, Subscription subscription, Payment payment)
            throws PaymentException {
        try {
            log.info("Creating Stripe payment link for subscription: {}", subscription.getId());

            // For demo, creating a mock checkout URL
            // In production, use Stripe Checkout Session API

            String checkoutUrl = "https://checkout.stripe.com/c/pay/mock_session_" +
                System.currentTimeMillis();

            log.info("Stripe checkout URL created successfully");
            return checkoutUrl;

        } catch (Exception e) {
            log.error("Error creating Stripe payment link", e);
            throw new PaymentException("Failed to create Stripe payment link: " + e.getMessage());
        }
    }

    /**
     * Get Stripe publishable key
     */
    public String getStripePublishableKey() {
        return stripePublishableKey;
    }

    /**
     * Check if Stripe is configured
     */
    public boolean isConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isEmpty()
               && stripePublishableKey != null && !stripePublishableKey.isEmpty();
    }
}
