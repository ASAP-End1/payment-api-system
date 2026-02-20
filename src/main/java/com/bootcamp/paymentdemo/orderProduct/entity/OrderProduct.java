package com.bootcamp.paymentdemo.orderProduct.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.order.entity.Order;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "order_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal orderPrice;

    @Column(nullable = false)
    private int count;

    @Builder
    public OrderProduct(Order order, Long productId, String productName, BigDecimal orderPrice, int count) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.orderPrice = orderPrice;
        this.count = count;
    }

    public BigDecimal getTotalAmount() {
        return orderPrice.multiply(BigDecimal.valueOf(count));
    }
}
