package com.boschtech.orderservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrors_shouldReturn400WithFieldErrors() {
        var binding = new BeanPropertyBindingResult(new Object(), "order");
        binding.addError(new FieldError("order", "productId", "Product ID is required"));
        binding.addError(new FieldError("order", "quantity", "Quantity must be positive"));
        var ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("error"));
        assertInstanceOf(java.util.List.class, response.getBody().get("details"));
    }

    @Test
    void handleMalformedJson_shouldReturn400() {
        var ex = new HttpMessageNotReadableException("bad json");

        ResponseEntity<Map<String, String>> response = handler.handleMalformedJson(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_shouldReturn400WithMessage() {
        var ex = new IllegalArgumentException("Product not found: xyz");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Product not found: xyz", response.getBody().get("error"));
    }

    @Test
    void handleGeneric_shouldReturn500WithoutStackTrace() {
        var ex = new RuntimeException("unexpected");

        ResponseEntity<Map<String, String>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().get("error"));
        assertFalse(response.getBody().get("error").contains("unexpected"));
    }
}
