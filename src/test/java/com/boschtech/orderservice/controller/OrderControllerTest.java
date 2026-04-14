package com.boschtech.orderservice.controller;

import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order createTestOrder() {
        Order order = new Order("product-1", "Wireless Keyboard", 2, new BigDecimal("159.98"));
        order.setId("test-order-123");
        order.setStatus("CONFIRMED");
        return order;
    }

    // --- GET /api/orders ---

    @Test
    void getAllOrders_shouldReturnListOfOrders() throws Exception {
        Order order = createTestOrder();
        when(orderService.getAllOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Wireless Keyboard"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void getAllOrders_shouldReturnEmptyList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/orders/{id} ---

    @Test
    void getOrderById_shouldReturnOrderWhenExists() throws Exception {
        Order order = createTestOrder();
        when(orderService.getOrderById("test-order-123")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/test-order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Wireless Keyboard"))
                .andExpect(jsonPath("$.id").value("test-order-123"));
    }

    @Test
    void getOrderById_shouldReturn404WhenNotExists() throws Exception {
        when(orderService.getOrderById("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/non-existent"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/orders ---

    @Test
    void createOrder_shouldReturn201WithCreatedOrder() throws Exception {
        Order order = createTestOrder();
        when(orderService.createOrder(any(Order.class))).thenReturn(order);

        String json = objectMapper.writeValueAsString(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Wireless Keyboard"));
    }

    @Test
    void createOrder_shouldReturn400WhenProductIdIsBlank() throws Exception {
        Order order = new Order();
        order.setProductId("");
        order.setQuantity(1);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenQuantityIsNull() throws Exception {
        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenQuantityIsNegative() throws Exception {
        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(-1);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenQuantityIsZero() throws Exception {
        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(0);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenProductNotFound() throws Exception {
        when(orderService.createOrder(any(Order.class)))
                .thenThrow(new IllegalArgumentException("Product not found: missing-product"));

        Order order = new Order();
        order.setProductId("missing-product");
        order.setQuantity(1);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/orders/product/{productId} ---

    @Test
    void getOrdersByProductId_shouldReturnOrdersForProduct() throws Exception {
        Order order = createTestOrder();
        when(orderService.getOrdersByProductId("product-1")).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/product/product-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("product-1"))
                .andExpect(jsonPath("$[0].productName").value("Wireless Keyboard"));
    }

    @Test
    void getOrdersByProductId_shouldReturnEmptyListWhenNoOrders() throws Exception {
        when(orderService.getOrdersByProductId("no-orders")).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/product/no-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
