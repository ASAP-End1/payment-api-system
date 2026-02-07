package com.bootcamp.paymentdemo.user.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SignupResponse {

    private Boolean success;
    private String message;

    // 정적 팩토리 메서드 - 성공 응답
    public static SignupResponse success() {
        return new SignupResponse(true, null);
    }

    // 정적 팩토리 메서드 - 실패 응답
    public static SignupResponse failure(String message) {
        return new SignupResponse(false, message);
    }
}
