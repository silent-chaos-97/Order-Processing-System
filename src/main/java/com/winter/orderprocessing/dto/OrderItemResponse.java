package com.winter.orderprocessing.dto;

import java.math.BigDecimal;
import java.util.UUID;

// A single line item as returned to API clients
public record OrderItemResponse(
        UUID id,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
