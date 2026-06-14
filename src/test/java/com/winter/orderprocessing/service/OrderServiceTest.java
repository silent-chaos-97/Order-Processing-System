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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private final OrderMapper orderMapper = new OrderMapper();

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderMapper);
    }

    private Order pendingOrder() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrderItem item = OrderItem.builder()
                .productName("Widget")
                .quantity(2)
                .unitPrice(new BigDecimal("9.99"))
                .build();
        order.addItem(item);
        order.recalculateTotal();
        return order;
    }

    @Test
    void createOrder_savesOrderWithComputedTotal() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Jane Doe",
                "jane@example.com",
                List.of(
                        new OrderItemRequest("Widget", 2, new BigDecimal("9.99")),
                        new OrderItemRequest("Gadget", 1, new BigDecimal("19.99"))
                )
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("39.97");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getItems()).allMatch(item -> item.getOrder() == captor.getValue());
    }

    @Test
    void getOrder_returnsOrder_whenFound() {
        Order order = pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(order.getId());

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.customerName()).isEqualTo("Jane Doe");
    }

    @Test
    void getOrder_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listOrders_filtersByStatus_whenProvided() {
        Order order = pendingOrder();
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.listOrders(OrderStatus.PENDING);

        assertThat(responses).hasSize(1);
        verify(orderRepository).findByStatus(OrderStatus.PENDING);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void listOrders_returnsAll_whenNoFilter() {
        when(orderRepository.findAll()).thenReturn(List.of(pendingOrder(), pendingOrder()));

        List<OrderResponse> responses = orderService.listOrders(null);

        assertThat(responses).hasSize(2);
        verify(orderRepository).findAll();
    }

    @Test
    void updateStatus_allowsValidTransition() {
        Order order = pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(order.getId(), OrderStatus.PROCESSING);

        assertThat(response.status()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateStatus_rejectsInvalidTransition() {
        Order order = pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), OrderStatus.DELIVERED))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_succeeds_whenPending() {
        Order order = pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_rejected_whenNotPending() {
        Order order = pendingOrder();
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void processPendingOrders_promotesAllPendingOrdersToProcessing() {
        Order first = pendingOrder();
        Order second = pendingOrder();
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(first, second));
        when(orderRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int updated = orderService.processPendingOrders();

        assertThat(updated).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(second.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }
}
