package com.zosh.service.impl;

import com.zosh.domain.PaymentGateway;
import com.zosh.domain.PaymentStatus;
import com.zosh.event.PaymentFailedEvent;
import com.zosh.event.PaymentInitiatedEvent;
import com.zosh.event.PaymentSuccessEvent;
import com.zosh.event.publisher.PaymentEventPublisher;
import com.zosh.exception.PaymentException;
import com.zosh.exception.UserException;
import com.zosh.mapper.PaymentMapper;

import com.zosh.modal.Payment;
import com.zosh.modal.Store;
import com.zosh.modal.Subscription;
import com.zosh.modal.User;
import com.zosh.payload.dto.PaymentDTO;
import com.zosh.payload.request.PaymentInitiateRequest;
import com.zosh.payload.request.PaymentVerifyRequest;
import com.zosh.payload.response.PaymentInitiateResponse;
import com.zosh.payload.response.PaymentLinkResponse;

import com.zosh.repository.PaymentRepository;

import com.zosh.repository.StoreRepository;
import com.zosh.repository.SubscriptionRepository;
import com.zosh.service.PaymentService;
import com.zosh.service.UserService;
import com.zosh.service.gateway.RazorpayService;
import com.zosh.service.gateway.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final RazorpayService razorpayService;
    private final UserService userService;
    private final StripeService stripeService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final StoreRepository storeRepository;
    private final SubscriptionRepository subscriptionRepository;


    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) throws PaymentException {
        try {
            User currentUser = userService.getCurrentUser();
            Store store = storeRepository.findByStoreAdminId(currentUser.getId());


            // Validate booking exists
            Subscription subscription=subscriptionRepository.findById(request.getSubscriptionId())
                            .orElseThrow(()->new PaymentException("Subscription not found"));

            // Check if payment already exists for this booking
            paymentRepository.findBySubscriptionId(request.getSubscriptionId())
                    .ifPresent(existingPayment -> {
                        if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                            try {
                                throw new PaymentException("Payment already completed for this booking");
                            } catch (PaymentException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

            // Create payment entity
            Payment payment = Payment.builder()
                    .store(store)
                    .subscription(subscription)
                    .amount(request.getAmount())
                    .provider(request.getGateway())
                    .status(PaymentStatus.PENDING)
                    .transactionId(generateTransactionId())
                    .build();

            payment = paymentRepository.save(payment);

            // Create response based on gateway
            PaymentInitiateResponse response = PaymentInitiateResponse.builder()
                    .paymentId(payment.getId())
                    .gateway(request.getGateway())
                    .transactionId(payment.getTransactionId())
                    .amount(request.getAmount())

                    .description(request.getDescription())
                    .success(true)
                    .message("Payment initiated successfully")
                    .build();

            if (request.getGateway() == PaymentGateway.RAZORPAY) {
                // In a real implementation, you would call Razorpay API to create order
                String razorpayOrderId = "order_" + UUID.randomUUID().toString().substring(0, 14);
                response.setRazorpayOrderId(razorpayOrderId);
                PaymentLinkResponse paymentLinkResponse= razorpayService.createPaymentLink(currentUser,payment);
                response.setCheckoutUrl(paymentLinkResponse.getPayment_link_url());
                response.setRazorpayOrderId(paymentLinkResponse.getPayment_link_id());
            } else if (request.getGateway() == PaymentGateway.STRIPE) {
                // In a real implementation, you would call Stripe API to create payment intent
                String checkoutUrl = "https://checkout.stripe.com/pay/" + payment.getTransactionId();
                response.setCheckoutUrl(checkoutUrl);
            }

            // Publish payment initiated event
            publishPaymentInitiatedEvent(payment, response.getCheckoutUrl());

            log.info("Payment initiated successfully with ID: {}", payment.getId());
            return response;

        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage(), e);
            throw new PaymentException("Failed to initiate payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentDTO verifyPayment(PaymentVerifyRequest request) throws PaymentException
    {

        // gatway payment
        JSONObject paymentDetails = razorpayService
                .fetchPaymentDetails(request.getRazorpayPaymentId());

        System.out.println("gatway payment details: " + paymentDetails);

        long amount = paymentDetails.getLong("amount");


        // Extract 'notes' object
        JSONObject notes = paymentDetails.getJSONObject("notes");

        // Access specific fields inside 'notes'

        Long paymentId = Long.parseLong(notes.optString("payment_id"));


        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with ID: " + paymentId));

//            if (payment.getStatus() == PaymentStatus.SUCCESS) {
//                log.warn("Payment already completed: {}", payment.getId());
//                return PaymentMapper.toDTO(payment);
//            }

        boolean isValid = razorpayService.isValidPayment(
                request.getRazorpayPaymentId());

        if (payment.getProvider() == PaymentGateway.RAZORPAY) {

            if (isValid) {
                payment.setProviderPaymentId(request.getRazorpayPaymentId());

            }
        } else if (payment.getProvider() == PaymentGateway.STRIPE) {
            isValid = stripeService.verifyPayment(request.getStripePaymentIntentId());

            if (isValid) {
                payment.setProviderPaymentId(request.getStripePaymentIntentId());
            }
        }

        if (isValid) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            log.info("Payment verified successfully: {}", payment.getId());

            // Save payment first
            payment = paymentRepository.save(payment);

            // Publish payment success event (instead of direct service calls)
            publishPaymentSuccessEvent(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment verification failed");
            log.error("Payment verification failed: {}", payment.getId());
            payment = paymentRepository.save(payment);

            // Publish payment failed event
            publishPaymentFailedEvent(payment);
        }

        return PaymentMapper.toDTO(payment);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getAllPayments(Pageable pageable) throws UserException {
        User currentUser = userService.getCurrentUser();
        Store store = storeRepository.findByStoreAdminId(currentUser.getId());

        if (store == null) {
            throw new UserException("Current user is not associated with any ");
        }



        // Only return payments for bookings on this airline's flights
        return paymentRepository.findByStoreId(store.getId(), pageable)
                .map(PaymentMapper::toDTO);
    }



    // Helper methods




    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Publish payment initiated event to notify other services.
     * This can be used for tracking and sending initial notifications.
     *
     * @param payment The initiated payment
     * @param checkoutUrl The URL for user to complete payment
     */
    private void publishPaymentInitiatedEvent(Payment payment, String checkoutUrl) {
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentId(payment.getId())
                .storeId(payment.getStore().getId())
                .provider(payment.getProvider())
                .amount(payment.getAmount())
                .subscriptionId(payment.getSubscription() != null ? payment.getSubscription().getId() : null)
                .transactionId(payment.getTransactionId())
                .initiatedAt(LocalDateTime.now()) // Fixed: use current time, not paidAt which is null
                .description("Payment for subscription: " + (payment.getSubscription() != null ? payment.getSubscription().getPlan().getName() : "N/A"))
                .checkoutUrl(checkoutUrl)
                .storeName(payment.getStore().getBrand())
                .build();

        paymentEventPublisher.publishPaymentInitiated(event);
        log.info("Published PaymentInitiatedEvent for payment ID: {}", payment.getId());
    }

    /**
     * Publish payment success event to notify other services.
     * This decouples payment processing from domain-specific actions.
     *
     * @param payment The successful payment
     */
    private void publishPaymentSuccessEvent(Payment payment) {
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(payment.getId())
                .storeId(payment.getStore().getId())
                .amount(payment.getAmount())
                .subscriptionId(payment.getSubscription() != null ? payment.getSubscription().getId() : null)
                .providerPaymentId(payment.getProviderPaymentId())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .description("Successful payment for subscription: " + (payment.getSubscription() != null ? payment.getSubscription().getPlan().getName()
                         : "N/A"))
                .build();

        paymentEventPublisher.publishPaymentSuccess(event);
        log.info("Published PaymentSuccessEvent for payment ID: {}", payment.getId());
    }

    /**
     * Publish payment failed event to notify other services.
     * This allows services to react to failures (e.g., send notifications, log errors).
     *
     * @param payment The failed payment
     */
    private void publishPaymentFailedEvent(Payment payment) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .storeId(payment.getStore().getId())
                .amount(payment.getAmount())
                .subscriptionId(payment.getSubscription() != null ? payment.getSubscription().getId() : null)
                .failureReason(payment.getFailureReason())
                .providerPaymentId(payment.getProviderPaymentId())
                .transactionId(payment.getTransactionId())
                .failedAt(LocalDateTime.now())
                .description("Failed payment for subscription: " )

                .build();

        paymentEventPublisher.publishPaymentFailed(event);
        log.warn("Published PaymentFailedEvent for payment ID: {} - Reason: {}",
                payment.getId(), payment.getFailureReason());
    }
}
