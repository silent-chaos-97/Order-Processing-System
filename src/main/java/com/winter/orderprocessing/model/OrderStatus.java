package com.winter.orderprocessing.model;

// Lifecycle states an order can move through, from creation to delivery or cancellation
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
