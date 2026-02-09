package com.bootcamp.paymentdemo.orderProduct.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "orderProducts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long productId;   // 원본 상품 참조 ID
    private String productName; // 주문 당시 상품명 (기록용)
    private BigDecimal orderPrice; // 주문 당시 가격 (기록용)
    private int count;        // 수량

    @Builder
    public OrderProduct(Long orderId, Long productId, String productName, BigDecimal orderPrice, int count) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.orderPrice = orderPrice;
        this.count = count;
    }

    public BigDecimal getTotalAmount() {
        return orderPrice.multiply(BigDecimal.valueOf(count));
    }
}
