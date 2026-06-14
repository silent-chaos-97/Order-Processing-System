package com.winter.orderprocessing.repository;

import com.winter.orderprocessing.model.Order;
import com.winter.orderprocessing.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatus(OrderStatus status);
}
