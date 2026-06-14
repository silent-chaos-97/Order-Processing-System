package com.winter.orderprocessing.service;

import com.winter.orderprocessing.dto.CreateOrderRequest;
import com.winter.orderprocessing.dto.OrderItemRequest;
import com.winter.orderprocessing.dto.OrderResponse;
import com.winter.orderprocessing.exception.InvalidOrderStateException;
import com.winter.orderprocessing.exception.ResourceNotFoundException;
import com.winter.orderprocessing.mapper.OrderMapper;
import com.winter.orderprocessing.model.Order;
import com.winter.orderprocessing.model.OrderItem;
import com.winter.orderprocessing.model.OrderStatus;
import com.winter.orderprocessing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Core business logic for creating, retrieving and transitioning orders
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    // Defines which status changes are permitted from each current status
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    // Builds and persists a new order with its line items and computed total
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = OrderItem.builder()
                    .productName(itemRequest.productName())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .build();
            order.addItem(item);
        }
        order.recalculateTotal();

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    // Looks up a single order by id, throwing if not found
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return orderMapper.toResponse(findOrderOrThrow(orderId));
    }

    // Lists orders, optionally filtered to a single status
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(OrderStatus statusFilter) {
        List<Order> orders = statusFilter != null
                ? orderRepository.findByStatus(statusFilter)
                : orderRepository.findAll();

        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    // Moves an order to a new status if the transition is allowed
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = findOrderOrThrow(orderId);
        transitionStatus(order, newStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // Cancels an order, only allowed while it is still PENDING
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Order can only be cancelled while it is PENDING. Current status: " + order.getStatus());
        }
        transitionStatus(order, OrderStatus.CANCELLED);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    /**
     * Transitions all PENDING orders to PROCESSING. Invoked by the scheduled background job.
     */
    public int processPendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        Instant now = Instant.now();
        for (Order order : pendingOrders) {
            order.setStatus(OrderStatus.PROCESSING);
            order.setUpdatedAt(now);
        }
        orderRepository.saveAll(pendingOrders);
        return pendingOrders.size();
    }

    // Applies a status change after checking it against ALLOWED_TRANSITIONS
    private void transitionStatus(Order order, OrderStatus newStatus) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(order.getStatus());
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStateException(
                    "Cannot transition order from " + order.getStatus() + " to " + newStatus);
        }
        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
    }

    // Fetches an order by id or throws ResourceNotFoundException
    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }
}
