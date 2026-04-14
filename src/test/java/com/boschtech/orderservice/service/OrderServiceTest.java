package com.boschtech.orderservice.service;

import com.boschtech.orderservice.client.ProductClient;
import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.model.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductClient productClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(productClient);
    }

    @Test
    void init_shouldSeedOneOrder() {
        orderService.init();
        List<Order> orders = orderService.getAllOrders();
        assertEquals(1, orders.size());
        assertEquals("CONFIRMED", orders.get(0).getStatus());
    }

    @Test
    void getAllOrders_shouldReturnEmptyListWhenNoOrders() {
        List<Order> orders = orderService.getAllOrders();
        assertTrue(orders.isEmpty());
    }

    @Test
    void getOrderById_shouldReturnOrderWhenExists() {
        orderService.init();
        Order seeded = orderService.getAllOrders().get(0);

        Optional<Order> found = orderService.getOrderById(seeded.getId());

        assertTrue(found.isPresent());
        assertEquals(seeded.getId(), found.get().getId());
    }

    @Test
    void getOrderById_shouldReturnEmptyWhenNotExists() {
        Optional<Order> found = orderService.getOrderById("non-existent-id");
        assertTrue(found.isEmpty());
    }

    @Test
    void createOrder_shouldCreateOrderWhenProductExists() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Wireless Keyboard");
        product.setPrice(new BigDecimal("79.99"));

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));

        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(3);

        Order created = orderService.createOrder(order);

        assertNotNull(created.getId());
        assertEquals("Wireless Keyboard", created.getProductName());
        assertEquals(0, new BigDecimal("239.97").compareTo(created.getTotalPrice()));
        assertEquals("CONFIRMED", created.getStatus());
        assertEquals(1, orderService.getAllOrders().size());
    }

    @Test
    void createOrder_shouldThrowWhenProductNotFound() {
        when(productClient.getProductById("missing-product")).thenReturn(Optional.empty());

        Order order = new Order();
        order.setProductId("missing-product");
        order.setQuantity(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(order)
        );

        assertTrue(exception.getMessage().contains("Product not found"));
        assertTrue(orderService.getAllOrders().isEmpty());
    }

    @Test
    void getOrdersByProductId_shouldReturnMatchingOrders() {
        orderService.init(); // seeds one order with productId "seed-product-1"

        List<Order> found = orderService.getOrdersByProductId("seed-product-1");
        assertEquals(1, found.size());
        assertEquals("seed-product-1", found.get(0).getProductId());
    }

    @Test
    void getOrdersByProductId_shouldReturnEmptyWhenNoMatch() {
        orderService.init();

        List<Order> found = orderService.getOrdersByProductId("non-existent-product");
        assertTrue(found.isEmpty());
    }

    @Test
    void createMultipleOrders_shouldReturnAll() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("10.00"));

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));

        for (int i = 0; i < 3; i++) {
            Order order = new Order();
            order.setProductId("product-1");
            order.setQuantity(1);
            orderService.createOrder(order);
        }

        assertEquals(3, orderService.getAllOrders().size());
    }

    @Test
    void createOrder_shouldCalculateTotalPriceCorrectly() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Coffee Maker");
        product.setPrice(new BigDecimal("49.99"));

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));

        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(5);

        Order created = orderService.createOrder(order);

        assertEquals(0, new BigDecimal("249.95").compareTo(created.getTotalPrice()));
    }
}
