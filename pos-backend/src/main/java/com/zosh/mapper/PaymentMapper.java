package com.zosh.mapper;

import com.zosh.modal.Payment;
import com.zosh.payload.dto.PaymentDTO;

import java.time.ZoneId;

public class PaymentMapper {

    public static PaymentDTO toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setGateway(payment.getProvider());
        dto.setAmount(payment.getAmount() != null ? payment.getAmount().longValue() : null);
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaymentMethod(payment.getMethod());
        dto.setStatus(payment.getStatus());

        if (payment.getSubscription() != null) {
            dto.setSubscriptionId(payment.getSubscription().getId());
            dto.setStoreId(payment.getStore() != null ?
                payment.getSubscription().getStore().getId() : null);
            dto.setStoreName(payment.getStore() != null ?
                payment.getStore().getBrand() : null);

        }

        if (payment.getPaidAt() != null) {
            dto.setCompletedAt(payment.getPaidAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());
        }

        if (payment.getCreatedAt() != null) {
            dto.setCreatedAt(payment.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());
            dto.setInitiatedAt(payment.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());
        }

        if (payment.getUpdatedAt() != null) {
            dto.setUpdatedAt(payment.getUpdatedAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());
        }

        return dto;
    }
}
