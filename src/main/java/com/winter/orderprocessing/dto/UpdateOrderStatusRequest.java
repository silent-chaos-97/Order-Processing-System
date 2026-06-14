package com.winter.orderprocessing.dto;

import com.winter.orderprocessing.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
