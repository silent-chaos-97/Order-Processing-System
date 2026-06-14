package com.winter.orderprocessing.mapper;

import com.winter.orderprocessing.dto.OrderItemResponse;
import com.winter.orderprocessing.dto.OrderResponse;
import com.winter.orderprocessing.model.Order;
import com.winter.orderprocessing.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

// Converts between entity classes and their API response DTOs
@Component
public class OrderMapper {

    // Maps an order and its items to the API response shape
    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }

    // Maps a single order item to its response shape, including the computed subtotal
    public OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
