package com.boschtech.orderservice.client;

import com.boschtech.orderservice.model.ProductDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class ProductClient {

    private final RestClient restClient;

    @Autowired
    public ProductClient(@Value("${app.product-service.url}") String productServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    public Optional<ProductDto> getProductById(String productId) {
        try {
            ProductDto product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }
}
