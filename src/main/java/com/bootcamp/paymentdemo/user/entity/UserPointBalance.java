package com.bootcamp.paymentdemo.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_point_balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPointBalance {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "current_points", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal currentPoints = BigDecimal.ZERO;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // 회원가입시 유저 포인트 초기화
    public static UserPointBalance createDefault(User user) {
        return UserPointBalance.builder()
                .user(user)
                .currentPoints(BigDecimal.ZERO)
                .build();
    }

    // 잔여 포인트 스냅샷 정합성 보정
    public void syncPointBalance(BigDecimal amount) {
        this.currentPoints = amount;
    }

    // 잔여 포인트 스냅샷 업데이트
    public void updatePointBalance(BigDecimal amount) {
        this.currentPoints = this.currentPoints.add(amount);
    }
}
