package com.boschtech.orderservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider verification test: order-service verifies it satisfies
 * the contract defined by product-service (the consumer).
 * Loads pact from product-service's target/pacts directory.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("order_service")
@PactFolder("../product-service/target/pacts")
class OrderServiceProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("orders exist for product product-001")
    void setupOrdersExist() {
        // Clear existing orders and add a known order
        Map<String, Order> orders = getOrdersMap();
        orders.clear();

        Order order = new Order("product-001", "Wireless Keyboard", 2, new BigDecimal("159.98"));
        order.setId("order-001");
        order.setStatus("CONFIRMED");
        orders.put(order.getId(), order);
    }

    @State("no orders exist for product no-orders-product")
    void setupNoOrders() {
        // Clear all orders so the product has none
        Map<String, Order> orders = getOrdersMap();
        orders.clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Order> getOrdersMap() {
        return (ConcurrentHashMap<String, Order>) ReflectionTestUtils.getField(orderService, "orders");
    }
}
