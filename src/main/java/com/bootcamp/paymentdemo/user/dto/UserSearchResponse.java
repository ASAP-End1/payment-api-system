package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserSearchResponse {
    private Long userId;
    private String email;
    private String name;
    private String phone;

    private BigDecimal pointBalance;     // 포인트 정보
    private BigDecimal totalPaidAmount;   // 총 결제 금액
    private String currentGrade;          // 현재 멤버십 등급 정보

    // 정적 팩토리 메서드
    public static UserSearchResponse from(User user, UserPointBalance userPointBalance, UserPaidAmount userpaidAmount) {
        return new UserSearchResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getPhoneNumber(),
                userPointBalance.getCurrentPoints(),
                userpaidAmount.getTotalPaidAmount(),
                user.getCurrentGrade().getGradeName().name()
        );
    }

}
