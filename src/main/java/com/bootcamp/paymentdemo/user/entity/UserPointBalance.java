package com.bootcamp.paymentdemo.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
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
}
