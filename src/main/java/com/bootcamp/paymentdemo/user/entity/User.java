package com.bootcamp.paymentdemo.user.entity;

import com.bootcamp.paymentdemo.common.entity.Base;
import com.bootcamp.paymentdemo.membership.entity.Membership;
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



    // 사용자 멤버십 등급 업데이트
    public void updateGrade(Membership grade) {
        this.currentGrade = grade;
    }

}

