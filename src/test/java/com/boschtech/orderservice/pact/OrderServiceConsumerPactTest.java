package com.boschtech.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer contract test: order-service defines what it expects from product-service.
 * Generates pact file: order_service-product_service.json
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "product_service")
class OrderServiceConsumerPactTest {

    @Pact(provider = "product_service", consumer = "order_service")
    V4Pact getProductByIdPact(PactDslWithProvider builder) {
        return builder
                .given("a product with ID product-001 exists")
                .uponReceiving("a request to get product by ID")
                .path("/api/products/product-001")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("id", "product-001")
                        .stringType("name", "Wireless Keyboard")
                        .stringType("description", "Bluetooth mechanical keyboard")
                        .decimalType("price", 79.99)
                        .stringType("category", "Electronics")
                        .booleanType("inStock", true))
                .toPact(V4Pact.class);
    }

    @Pact(provider = "product_service", consumer = "order_service")
    V4Pact getProductNotFoundPact(PactDslWithProvider builder) {
        return builder
                .given("no product with ID missing-product exists")
                .uponReceiving("a request to get a non-existent product")
                .path("/api/products/missing-product")
                .method("GET")
                .willRespondWith()
                .status(404)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getProductByIdPact")
    void testGetProductById(MockServer mockServer) {
        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.getUrl())
                .build();

        Map response = restClient.get()
                .uri("/api/products/product-001")
                .retrieve()
                .body(Map.class);

        assertThat(response).isNotNull();
        assertThat(response.get("id")).isEqualTo("product-001");
        assertThat(response.get("name")).isEqualTo("Wireless Keyboard");
        assertThat(response.get("price")).isNotNull();
        assertThat(response.get("inStock")).isEqualTo(true);
    }

    @Test
    @PactTestFor(pactMethod = "getProductNotFoundPact")
    void testGetProductNotFound(MockServer mockServer) {
        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.getUrl())
                .build();

        try {
            restClient.get()
                    .uri("/api/products/missing-product")
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            // 404 throws an exception — this is expected
            assertThat(e.getMessage()).contains("404");
        }
    }
}
