package com.boschtech.orderservice.service;

import com.boschtech.orderservice.client.ProductClient;
import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.model.ProductDto;
import com.boschtech.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productClient);
    }

    @Test
    void init_shouldSeedOneOrderWhenEmpty() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.init();

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void init_shouldNotSeedWhenOrdersExist() {
        when(orderRepository.count()).thenReturn(1L);

        orderService.init();

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getAllOrders_shouldReturnEmptyListWhenNoOrders() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<Order> orders = orderService.getAllOrders();
        assertTrue(orders.isEmpty());
    }

    @Test
    void getOrderById_shouldReturnOrderWhenExists() {
        Order order = new Order("product-1", "Test", 1, new BigDecimal("10.00"));
        order.setId("test-order-id");
        when(orderRepository.findById("test-order-id")).thenReturn(Optional.of(order));

        Optional<Order> found = orderService.getOrderById("test-order-id");

        assertTrue(found.isPresent());
        assertEquals("test-order-id", found.get().getId());
    }

    @Test
    void getOrderById_shouldReturnEmptyWhenNotExists() {
        when(orderRepository.findById("non-existent-id")).thenReturn(Optional.empty());

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
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(3);

        Order created = orderService.createOrder(order);

        assertEquals("Wireless Keyboard", created.getProductName());
        assertEquals(0, new BigDecimal("239.97").compareTo(created.getTotalPrice()));
        assertEquals("CONFIRMED", created.getStatus());
        verify(orderRepository).save(order);
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
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersByProductId_shouldReturnMatchingOrders() {
        Order order = new Order("seed-product-1", "Sample Product", 2, new BigDecimal("159.98"));
        when(orderRepository.findByProductId("seed-product-1")).thenReturn(List.of(order));

        List<Order> found = orderService.getOrdersByProductId("seed-product-1");
        assertEquals(1, found.size());
        assertEquals("seed-product-1", found.get(0).getProductId());
    }

    @Test
    void getOrdersByProductId_shouldReturnEmptyWhenNoMatch() {
        when(orderRepository.findByProductId("non-existent-product")).thenReturn(List.of());

        List<Order> found = orderService.getOrdersByProductId("non-existent-product");
        assertTrue(found.isEmpty());
    }

    @Test
    void createMultipleOrders_shouldSaveAll() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("10.00"));

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < 3; i++) {
            Order order = new Order();
            order.setProductId("product-1");
            order.setQuantity(1);
            orderService.createOrder(order);
        }

        verify(orderRepository, times(3)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldNullifyBlankId() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("10.00"));

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(1);
        order.setId("   ");

        Order created = orderService.createOrder(order);

        assertNull(created.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void createOrder_shouldPreserveNonBlankId() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("10.00"));

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(1);
        order.setId("explicit-id");

        Order created = orderService.createOrder(order);

        assertEquals("explicit-id", created.getId());
    }

    @Test
    void createOrder_shouldCalculateTotalPriceCorrectly() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Coffee Maker");
        product.setPrice(new BigDecimal("49.99"));

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = new Order();
        order.setProductId("product-1");
        order.setQuantity(5);

        Order created = orderService.createOrder(order);

        assertEquals(0, new BigDecimal("249.95").compareTo(created.getTotalPrice()));
    }
}
