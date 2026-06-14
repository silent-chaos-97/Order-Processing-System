package com.winter.orderprocessing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.orderprocessing.dto.CreateOrderRequest;
import com.winter.orderprocessing.dto.OrderItemRequest;
import com.winter.orderprocessing.dto.OrderResponse;
import com.winter.orderprocessing.dto.UpdateOrderStatusRequest;
import com.winter.orderprocessing.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Builds a valid order request reused across tests
    private CreateOrderRequest validRequest() {
        return new CreateOrderRequest(
                "Jane Doe",
                "jane@example.com",
                List.of(
                        new OrderItemRequest("Widget", 2, new BigDecimal("9.99")),
                        new OrderItemRequest("Gadget", 1, new BigDecimal("19.99"))
                )
        );
    }

    // Creates an order via the API and returns its generated id
    private UUID createOrder() throws Exception {
        String body = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(body, OrderResponse.class).id();
    }

    // Creating an order should return 201 with status PENDING and a computed total
    @Test
    void createOrder_returnsCreatedOrderWithComputedTotal() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.totalAmount", is(39.97)))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    // Invalid payloads should fail validation with field-level errors
    @Test
    void createOrder_rejectsInvalidPayload() throws Exception {
        CreateOrderRequest invalid = new CreateOrderRequest("", "not-an-email", List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerName", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.customerEmail", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.items", notNullValue()));
    }

    // Fetching an existing order by id should return it
    @Test
    void getOrder_returnsOrder_whenFound() throws Exception {
        UUID id = createOrder();

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())));
    }

    // Fetching a non-existent order should return 404
    @Test
    void getOrder_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // Listing orders by status should only return orders matching that status
    @Test
    void listOrders_filtersByStatus() throws Exception {
        createOrder();

        mockMvc.perform(get("/api/orders").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        mockMvc.perform(get("/api/orders").param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    // PENDING -> PROCESSING is a valid transition and should succeed
    @Test
    void updateStatus_allowsValidTransition() throws Exception {
        UUID id = createOrder();

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.PROCESSING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSING")));
    }

    // PENDING -> DELIVERED skips required states and should be rejected
    @Test
    void updateStatus_rejectsInvalidTransition() throws Exception {
        UUID id = createOrder();

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isConflict());
    }

    // A PENDING order can be cancelled
    @Test
    void cancelOrder_succeeds_whenPending() throws Exception {
        UUID id = createOrder();

        mockMvc.perform(post("/api/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    // Once an order moves past PENDING, it can no longer be cancelled
    @Test
    void cancelOrder_rejected_whenNotPending() throws Exception {
        UUID id = createOrder();

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.PROCESSING))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/cancel", id))
                .andExpect(status().isConflict());
    }
}
