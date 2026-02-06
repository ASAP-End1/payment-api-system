package com.bootcamp.paymentdemo.point.entity;

import com.bootcamp.paymentdemo.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "point_usages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PointUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_transaction_id", nullable = false)
    private PointTransaction pointTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private int amount;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime usedAt;

    public PointUsage(PointTransaction pointTransaction, Order order, int amount) {
        this.pointTransaction = pointTransaction;
        this.order = order;
        this.amount = amount;
    }
}
