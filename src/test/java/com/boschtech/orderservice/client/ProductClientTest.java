package com.boschtech.orderservice.client;

import com.boschtech.orderservice.model.ProductDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link ProductClient} against an in-process HTTP server so we can
 * exercise the full {@code RestClient} pipeline (baseUrl, URI template,
 * deserialization) without a network dependency.
 */
class ProductClientTest {

    private HttpServer server;
    private int port;
    private AtomicReference<Integer> nextStatus;
    private AtomicReference<String> nextBody;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        nextStatus = new AtomicReference<>(200);
        nextBody = new AtomicReference<>("");

        server.createContext("/api/products/", exchange -> {
            byte[] bytes = nextBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(nextStatus.get(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.close();
            }
        });

        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private ProductClient clientFor(String baseUrl) {
        return new ProductClient(baseUrl);
    }

    @Test
    void getProductById_returnsProduct_on200() {
        nextStatus.set(200);
        nextBody.set(
                "{"
                        + "\"id\":\"prod-42\","
                        + "\"name\":\"Mechanical Keyboard\","
                        + "\"description\":\"Tactile switches\","
                        + "\"price\":129.95,"
                        + "\"category\":\"Peripherals\","
                        + "\"inStock\":true"
                        + "}"
        );

        Optional<ProductDto> result = clientFor("http://127.0.0.1:" + port)
                .getProductById("prod-42");

        assertTrue(result.isPresent());
        ProductDto dto = result.get();
        assertEquals("prod-42", dto.getId());
        assertEquals("Mechanical Keyboard", dto.getName());
        assertEquals("Tactile switches", dto.getDescription());
        assertNotNull(dto.getPrice());
        assertEquals(0, dto.getPrice().compareTo(new java.math.BigDecimal("129.95")));
        assertEquals("Peripherals", dto.getCategory());
        assertTrue(dto.isInStock());
    }

    @Test
    void getProductById_returnsEmpty_on404() {
        nextStatus.set(404);
        nextBody.set("");

        Optional<ProductDto> result = clientFor("http://127.0.0.1:" + port)
                .getProductById("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void getProductById_returnsEmpty_on500() {
        nextStatus.set(500);
        nextBody.set("");

        Optional<ProductDto> result = clientFor("http://127.0.0.1:" + port)
                .getProductById("boom");

        assertTrue(result.isEmpty());
    }

    @Test
    void getProductById_returnsEmpty_whenBodyIsEmptyWith200() {
        // 200 OK but no JSON body -> RestClient returns null -> Optional.empty().
        nextStatus.set(200);
        nextBody.set("");

        Optional<ProductDto> result = clientFor("http://127.0.0.1:" + port)
                .getProductById("ghost");

        assertTrue(result.isEmpty());
    }

    @Test
    void getProductById_returnsEmpty_whenHostUnreachable() {
        // Point the client at a closed port so the HTTP call fails with a
        // ResourceAccessException (subtype of RestClientException).
        server.stop(0);

        Optional<ProductDto> result = clientFor("http://127.0.0.1:" + port)
                .getProductById("any");

        assertTrue(result.isEmpty());
    }
}
