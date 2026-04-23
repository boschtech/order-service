package com.boschtech.orderservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductDtoTest {

    @Test
    void defaultConstructor_shouldCreateEmptyDto() {
        ProductDto dto = new ProductDto();

        assertNull(dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getPrice());
        assertNull(dto.getCategory());
        assertFalse(dto.isInStock());
    }

    @Test
    void settersAndGetters_shouldRoundTripAllFields() {
        ProductDto dto = new ProductDto();

        dto.setId("prod-1");
        dto.setName("Wireless Mouse");
        dto.setDescription("Ergonomic 2.4GHz mouse");
        dto.setPrice(new BigDecimal("29.99"));
        dto.setCategory("Peripherals");
        dto.setInStock(true);

        assertEquals("prod-1", dto.getId());
        assertEquals("Wireless Mouse", dto.getName());
        assertEquals("Ergonomic 2.4GHz mouse", dto.getDescription());
        assertEquals(0, new BigDecimal("29.99").compareTo(dto.getPrice()));
        assertEquals("Peripherals", dto.getCategory());
        assertTrue(dto.isInStock());
    }

    @Test
    void setInStock_shouldFlipFalseToTrueAndBack() {
        ProductDto dto = new ProductDto();

        dto.setInStock(true);
        assertTrue(dto.isInStock());

        dto.setInStock(false);
        assertFalse(dto.isInStock());
    }

    @Test
    void settersAcceptNullValues() {
        ProductDto dto = new ProductDto();
        dto.setId("x");
        dto.setName("x");
        dto.setDescription("x");
        dto.setPrice(BigDecimal.ONE);
        dto.setCategory("x");

        dto.setId(null);
        dto.setName(null);
        dto.setDescription(null);
        dto.setPrice(null);
        dto.setCategory(null);

        assertNull(dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getPrice());
        assertNull(dto.getCategory());
    }
}
