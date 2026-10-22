package com.zosh.mapper;

import com.zosh.modal.Subscription;
import com.zosh.payload.dto.SubscriptionDTO;

public class SubscriptionMapper {

    public static SubscriptionDTO toDto(Subscription sub) {
        if (sub == null) {
            return null;
        }
        return SubscriptionDTO.builder()
                .id(sub.getId())
                .storeId(sub.getStore() != null ? sub.getStore().getId() : null)
                .storeName(sub.getStore() != null ? sub.getStore().getBrand() : null)
                .planName(sub.getPlan() != null ? sub.getPlan().getName() : null)
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .paymentStatus(sub.getPaymentStatus())
                .paymentGateway(sub.getPaymentGateway())
                .transactionId(sub.getTransactionId())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }
}
