package com.boschtech.orderservice.service;

import com.boschtech.orderservice.client.ProductClient;
import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.model.ProductDto;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final ProductClient productClient;

    public OrderService(ProductClient productClient) {
        this.productClient = productClient;
    }

    @PostConstruct
    public void init() {
        // Seed sample data (without product validation for seed data)
        Order sample = new Order("seed-product-1", "Sample Product", 2, new BigDecimal("159.98"));
        sample.setStatus("CONFIRMED");
        orders.put(sample.getId(), sample);
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public Optional<Order> getOrderById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order createOrder(Order order) {
        // Validate product exists via product-service
        Optional<ProductDto> product = productClient.getProductById(order.getProductId());
        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + order.getProductId());
        }

        ProductDto p = product.get();
        order.setProductName(p.getName());
        order.setTotalPrice(p.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
        order.setStatus("CONFIRMED");
        orders.put(order.getId(), order);
        return order;
    }

    public List<Order> getOrdersByProductId(String productId) {
        return orders.values().stream()
                .filter(o -> productId.equals(o.getProductId()))
                .collect(Collectors.toList());
    }
}
