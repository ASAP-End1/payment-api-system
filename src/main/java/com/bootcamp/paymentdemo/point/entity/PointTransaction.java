package com.bootcamp.paymentdemo.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "point_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO User 엔티티 연결
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
    private Long userId;

    // TODO Order 엔티티 연결
//    @ManyToOne(fetch = FetchType.LAZY, optional = true)
//    @JoinColumn(name = "order_id", nullable = true)
//    private Order order;
    private Long orderId;

    private int amount;

    @Enumerated(EnumType.STRING)
    private PointType type;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    // TODO User, Order 객체로 변경
    public PointTransaction(Long userId, Long orderId, int amount, PointType type, LocalDateTime expiresAt) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.type = type;
        this.expiresAt = expiresAt;
    }
}
