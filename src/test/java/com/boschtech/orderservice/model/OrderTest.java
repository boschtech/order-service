package com.boschtech.orderservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void defaultConstructor_shouldSetDefaultsWithNullId() {
        Order order = new Order();
        assertNull(order.getId());
        assertEquals("PENDING", order.getStatus());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void parameterizedConstructor_shouldSetAllFieldsExceptId() {
        Order order = new Order("product-1", "Keyboard", 2, new BigDecimal("159.98"));

        assertNull(order.getId());
        assertEquals("product-1", order.getProductId());
        assertEquals("Keyboard", order.getProductName());
        assertEquals(2, order.getQuantity());
        assertEquals(new BigDecimal("159.98"), order.getTotalPrice());
        assertEquals("PENDING", order.getStatus());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void setters_shouldUpdateFields() {
        Order order = new Order();

        order.setId("custom-id");
        order.setProductId("product-99");
        order.setProductName("Mouse");
        order.setQuantity(5);
        order.setTotalPrice(new BigDecimal("149.95"));
        order.setStatus("CONFIRMED");
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);

        assertEquals("custom-id", order.getId());
        assertEquals("product-99", order.getProductId());
        assertEquals("Mouse", order.getProductName());
        assertEquals(5, order.getQuantity());
        assertEquals(new BigDecimal("149.95"), order.getTotalPrice());
        assertEquals("CONFIRMED", order.getStatus());
        assertEquals(now, order.getCreatedAt());
    }

    @Test
    void idIsNullBeforePersistence() {
        Order o1 = new Order();
        Order o2 = new Order();
        assertNull(o1.getId());
        assertNull(o2.getId());
    }

    @Test
    void ensureId_shouldGenerateUuidWhenIdIsNull() {
        Order order = new Order();
        assertNull(order.getId());

        order.ensureId();

        assertNotNull(order.getId());
        assertFalse(order.getId().isEmpty());
    }

    @Test
    void ensureId_shouldPreserveExistingId() {
        Order order = new Order();
        order.setId("custom-id");

        order.ensureId();

        assertEquals("custom-id", order.getId());
    }

    @Test
    void ensureId_shouldGenerateUniqueIds() {
        Order o1 = new Order();
        Order o2 = new Order();
        o1.ensureId();
        o2.ensureId();
        assertNotEquals(o1.getId(), o2.getId());
    }
}
