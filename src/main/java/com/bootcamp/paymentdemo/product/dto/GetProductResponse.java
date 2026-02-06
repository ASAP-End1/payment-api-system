package com.bootcamp.paymentdemo.product.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class GetProductResponse {

    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final int stock;
    private final String category;
    private final String status;

    public GetProductResponse(Long id, String name, BigDecimal price, int stock, String category, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.status = status;
    }
}
