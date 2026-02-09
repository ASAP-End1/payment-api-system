package com.bootcamp.paymentdemo.domain.user.entity;

import com.bootcamp.paymentdemo.common.entity.Base;
import com.bootcamp.paymentdemo.domain.membership.entity.Membership;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_grade_id", nullable = false)
    private Membership currentGrade;

    // 정적 팩토리 메서드 - 회원가입용
    public static User register(
            String email,
            String encodedPassword,
            String username,
            String phoneNumber,
            Membership defaultGrade
    ) {
        User user = new User();

        user.email = email;
        user.password = encodedPassword;
        user.username = username;
        user.phoneNumber = phoneNumber;
        user.currentGrade = defaultGrade;

        return user;
    }

    // PortOne customerUid 생성
    public String generateCustomerUid() {
        // CUST_{userId}_{random6}
        int random6 = (int) (Math.random() * 900000) + 100000;
        return String.format("CUST_%d_%d", this.userId, random6);
    }


}

