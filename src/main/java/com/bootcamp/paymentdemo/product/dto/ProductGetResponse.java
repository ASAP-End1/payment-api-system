package com.bootcamp.paymentdemo.product.dto;

import com.bootcamp.paymentdemo.product.entity.Product;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ProductGetResponse {

    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final int stock;
    private final String category;
    private final String status;

    public ProductGetResponse(Long id, String name, BigDecimal price, int stock, String category, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.status = status;
    }


    public static ProductGetResponse from(Product product) {
        return new ProductGetResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getStatus().name()
        );
    }
}
