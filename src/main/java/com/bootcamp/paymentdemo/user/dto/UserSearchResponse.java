package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserSearchResponse {
    private String customerUid;  // PortOne 고객 고유 식별자
    private String email;
    private String name;
    private String phone;
    private Long pointBalance;  // number 타입 (소수점 제거)

    // 정적 팩토리 메서드
    public static UserSearchResponse from(User user, UserPointBalance userPointBalance) {
        return new UserSearchResponse(
                user.generateCustomerUid(),
                user.getEmail(),
                user.getUsername(),
                user.getPhoneNumber(),
                userPointBalance.getCurrentPoints().longValue()
        );
    }

}
