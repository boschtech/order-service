package com.boschtech.orderservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.boschtech.orderservice.client.ProductClient;
import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.model.ProductDto;
import com.boschtech.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Provider verification test: order-service verifies it satisfies
 * the contract defined by the micro-frontend (the consumer).
 * Loads pact from micro-frontend's pacts directory.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("order-service")
@PactFolder("../micro-frontend/pacts")
class MicroFrontendOrderProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

    @MockBean
    private ProductClient productClient;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("orders exist")
    void setupOrdersExist() {
        Map<String, Order> orders = getOrdersMap();
        orders.clear();

        Order order = new Order("prod-001", "Widget Alpha", 2, new BigDecimal("59.98"));
        order.setId("order-001");
        order.setStatus("CONFIRMED");
        orders.put(order.getId(), order);
    }

    @State("order order-001 exists")
    void setupOrderExists() {
        Map<String, Order> orders = getOrdersMap();
        orders.clear();

        Order order = new Order("prod-001", "Widget Alpha", 2, new BigDecimal("59.98"));
        order.setId("order-001");
        order.setStatus("CONFIRMED");
        orders.put(order.getId(), order);
    }

    @State("orders exist for product prod-001")
    void setupOrdersForProduct() {
        Map<String, Order> orders = getOrdersMap();
        orders.clear();

        Order order = new Order("prod-001", "Widget Alpha", 2, new BigDecimal("59.98"));
        order.setId("order-001");
        order.setStatus("CONFIRMED");
        orders.put(order.getId(), order);
    }

    @State("product prod-001 exists and is in stock")
    void setupProductExistsForOrderCreation() {
        // Clear existing orders so the new one can be created cleanly
        Map<String, Order> orders = getOrdersMap();
        orders.clear();

        // Mock the ProductClient so order-service can validate the product
        ProductDto product = new ProductDto();
        product.setId("prod-001");
        product.setName("Widget Alpha");
        product.setDescription("A premium widget for all your needs");
        product.setPrice(new BigDecimal("29.99"));
        product.setCategory("Widgets");
        product.setInStock(true);

        when(productClient.getProductById(eq("prod-001")))
                .thenReturn(Optional.of(product));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Order> getOrdersMap() {
        return (ConcurrentHashMap<String, Order>) ReflectionTestUtils.getField(orderService, "orders");
    }
}
