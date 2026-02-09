package com.bootcamp.paymentdemo.product.entity;

import com.bootcamp.paymentdemo.product.consts.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    private int stock;
    private String category;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    public Product(String name, BigDecimal price, int stock, String category, ProductStatus status) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.status = status;
    }

}
