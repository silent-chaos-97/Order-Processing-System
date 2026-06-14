package com.winter.orderprocessing.dto;

import com.winter.orderprocessing.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Full representation of an order returned to API clients
public record OrderResponse(
        UUID id,
        String customerName,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items
) {
}
