package com.winter.orderprocessing.scheduler;

import com.winter.orderprocessing.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusSchedulerTest {

    @Mock
    private OrderService orderService;

    @Test
    void promotePendingOrders_delegatesToOrderService() {
        when(orderService.processPendingOrders()).thenReturn(3);

        new OrderStatusScheduler(orderService).promotePendingOrders();

        verify(orderService).processPendingOrders();
    }
}
