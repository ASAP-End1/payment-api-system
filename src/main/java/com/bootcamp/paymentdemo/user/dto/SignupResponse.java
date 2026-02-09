package com.bootcamp.paymentdemo.user.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SignupResponse {

    private Boolean success;
    private String message;
    private Long userId;
    private String email;

    // 정적 팩토리 메서드 - 성공 응답
    public static SignupResponse success(Long userId, String email) {
        return new SignupResponse(true, "회원가입 성공", userId, email);
    }


}
