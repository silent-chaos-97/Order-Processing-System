package com.winter.orderprocessing.scheduler;

import com.winter.orderprocessing.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderService orderService;

    @Scheduled(fixedRateString = "${order-processing.pending-to-processing-interval-ms}")
    public void promotePendingOrders() {
        int updated = orderService.processPendingOrders();
        if (updated > 0) {
            log.info("Promoted {} order(s) from PENDING to PROCESSING", updated);
        }
    }
}
