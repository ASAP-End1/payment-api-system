package com.bootcamp.paymentdemo.product.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
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
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    public Product(String name, BigDecimal price, int stock, String category, ProductStatus status) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.status = status;
    }

    // 재고 차감 (주문 생성 시)
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 수량은 0보다 커야 합니다.");
        }
        if (this.stock < quantity) {
            throw new IllegalStateException(
                String.format("재고가 부족합니다. 상품: %s (현재 재고: %d, 요청 수량: %d)",
                    this.name, this.stock, quantity)
            );
        }
        this.stock -= quantity;
    }

    // 재고 복구 (주문 취소/환불 시)
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복구할 수량은 0보다 커야 합니다.");
        }
        this.stock += quantity;
    }
}