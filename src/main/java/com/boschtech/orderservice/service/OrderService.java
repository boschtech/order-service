package com.boschtech.orderservice.service;

import com.boschtech.orderservice.client.ProductClient;
import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.model.ProductDto;
import com.boschtech.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    @PostConstruct
    public void init() {
        if (orderRepository.count() == 0) {
            Order sample = new Order("seed-product-1", "Sample Product", 2, new BigDecimal("159.98"));
            sample.setStatus("CONFIRMED");
            orderRepository.save(sample);
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
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
        if (order.getId() != null && order.getId().isBlank()) {
            order.setId(null);
        }
        order.setStatus("CONFIRMED");
        return orderRepository.save(order);
    }

    public List<Order> getOrdersByProductId(String productId) {
        return orderRepository.findByProductId(productId);
    }
}
