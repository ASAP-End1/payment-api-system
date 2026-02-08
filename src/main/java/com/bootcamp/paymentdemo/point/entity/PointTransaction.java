package com.bootcamp.paymentdemo.point.entity;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "point_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_id", nullable = true)
    private Order order;

    private int amount;
    private int remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PointType type;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDate expiresAt;

    // TODO User 객체로 변경
    public PointTransaction(User user, Order order, int amount, PointType type) {
        this.user = user;
        this.order = order;
        this.amount = amount;
        this.type = type;

        // 적립일 때 남은 금액, 만료일 자동 설정
        if (type == PointType.EARNED) {
            this.remainingAmount = amount;
            this.expiresAt = LocalDate.now().plusYears(1);
        }
    }

    // 잔여 포인트 차감 메서드
    public void deduct(int amount) {
        this.remainingAmount -= amount;
    }

    // 잔여 포인트 복구 메서드
    public void restore(int amount) {
        this.remainingAmount += amount;
    }
}
