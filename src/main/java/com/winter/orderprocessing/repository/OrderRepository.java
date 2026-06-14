package com.winter.orderprocessing.repository;

import com.winter.orderprocessing.model.Order;
import com.winter.orderprocessing.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Data access for Order entities
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Finds all orders currently in the given status
    List<Order> findByStatus(OrderStatus status);
}
