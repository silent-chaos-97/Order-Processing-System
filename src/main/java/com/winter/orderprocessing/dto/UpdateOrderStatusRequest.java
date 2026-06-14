package com.winter.orderprocessing.dto;

import com.winter.orderprocessing.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

// Payload for requesting an order status change
public record UpdateOrderStatusRequest(

        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
