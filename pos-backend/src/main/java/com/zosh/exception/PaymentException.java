package com.zosh.exception;

/**
 * Custom exception for payment-related operations
 */
public class PaymentException extends Exception {

    public PaymentException(String message) {
        super(message);
    }


}
