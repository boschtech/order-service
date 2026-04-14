package com.boschtech.orderservice.component.tests;

import com.boschtech.orderservice.client.ProductClient;
import com.boschtech.orderservice.model.Order;
import com.boschtech.orderservice.model.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Component tests that spin up the full Spring Boot application on a random port
 * and interact with it over HTTP using WebTestClient — simulating how a
 * microfrontend would call the API.
 *
 * ProductClient is mocked to avoid requiring a running product-service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderApiComponentTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductClient productClient;

    // Stored across tests via static field (tests run in order)
    private static String createdOrderId;

    @BeforeEach
    void setUpMocks() {
        ProductDto product = new ProductDto();
        product.setId("product-1");
        product.setName("Wireless Keyboard");
        product.setDescription("Bluetooth mechanical keyboard");
        product.setPrice(new BigDecimal("79.99"));
        product.setCategory("Electronics");
        product.setInStock(true);

        when(productClient.getProductById("product-1")).thenReturn(Optional.of(product));
        when(productClient.getProductById("non-existent-product")).thenReturn(Optional.empty());
    }

    // ─── GET /api/orders ──────────────────────────────────────────

    @Test
    @org.junit.jupiter.api.Order(1)
    void shouldReturnSeededOrdersOnStartup() {
        webTestClient.get()
                .uri("/api/orders")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Order.class)
                .hasSize(1);
    }

    // ─── POST /api/orders ─────────────────────────────────────────

    @Test
    @org.junit.jupiter.api.Order(2)
    void shouldCreateNewOrder() {
        String json = """
                {
                    "productId": "product-1",
                    "quantity": 3
                }
                """;

        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Order.class)
                .value(order -> {
                    assertNotNull(order.getId());
                    assertEquals("product-1", order.getProductId());
                    assertEquals("Wireless Keyboard", order.getProductName());
                    assertEquals(3, order.getQuantity());
                    assertEquals(0, new BigDecimal("239.97").compareTo(order.getTotalPrice()));
                    assertEquals("CONFIRMED", order.getStatus());
                    // Store the ID for subsequent tests
                    createdOrderId = order.getId();
                });
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void shouldHaveTwoOrdersAfterCreation() {
        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .hasSize(2);
    }

    // ─── POST validation (bad requests) ─────────────────────────────

    @Test
    @org.junit.jupiter.api.Order(4)
    void shouldRejectOrderWithBlankProductId() {
        String json = """
                {
                    "productId": "",
                    "quantity": 1
                }
                """;

        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void shouldRejectOrderWithNegativeQuantity() {
        String json = """
                {
                    "productId": "product-1",
                    "quantity": -1
                }
                """;

        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void shouldRejectOrderWithZeroQuantity() {
        String json = """
                {
                    "productId": "product-1",
                    "quantity": 0
                }
                """;

        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    void shouldRejectOrderWhenProductNotFound() {
        String json = """
                {
                    "productId": "non-existent-product",
                    "quantity": 1
                }
                """;

        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ─── GET /api/orders/{id} ─────────────────────────────────────

    @Test
    @org.junit.jupiter.api.Order(8)
    void shouldGetOrderById() {
        assertNotNull(createdOrderId, "Order ID should have been set by create test");

        webTestClient.get()
                .uri("/api/orders/{id}", createdOrderId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class)
                .value(order -> {
                    assertEquals(createdOrderId, order.getId());
                    assertEquals("Wireless Keyboard", order.getProductName());
                });
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    void shouldReturn404ForNonExistentOrder() {
        webTestClient.get()
                .uri("/api/orders/{id}", "non-existent-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ─── GET /api/orders/product/{productId} ─────────────────────

    @Test
    @org.junit.jupiter.api.Order(10)
    void shouldGetOrdersByProductId() {
        webTestClient.get()
                .uri("/api/orders/product/{productId}", "product-1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .value(orders -> {
                    assertFalse(orders.isEmpty());
                    orders.forEach(o -> assertEquals("product-1", o.getProductId()));
                });
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    void shouldReturnEmptyListForProductWithNoOrders() {
        webTestClient.get()
                .uri("/api/orders/product/{productId}", "no-orders-product")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .hasSize(0);
    }

    // ─── Actuator health (used by microfrontend orchestrators) ──────

    @Test
    @org.junit.jupiter.api.Order(12)
    void shouldExposeHealthEndpoint() {
        webTestClient.get()
                .uri("/actuator/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    // ─── Content-Type negotiation ───────────────────────────────────

    @Test
    @org.junit.jupiter.api.Order(13)
    void shouldReturnJsonContentType() {
        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    // ─── Full create + retrieve lifecycle in a single test ──────────

    @Test
    @org.junit.jupiter.api.Order(14)
    void shouldSupportCreateAndRetrieveLifecycle() {
        // CREATE
        Order created = webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "productId": "product-1",
                            "quantity": 1
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(created);
        String id = created.getId();

        // READ
        webTestClient.get()
                .uri("/api/orders/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class)
                .value(o -> {
                    assertEquals("Wireless Keyboard", o.getProductName());
                    assertEquals("CONFIRMED", o.getStatus());
                });

        // VERIFY IN LIST BY PRODUCT
        webTestClient.get()
                .uri("/api/orders/product/product-1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .value(orders -> assertTrue(
                        orders.stream().anyMatch(o -> o.getId().equals(id))
                ));
    }
}
