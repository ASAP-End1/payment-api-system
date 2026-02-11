package com.bootcamp.paymentdemo.user.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LoginResponse {

    private Boolean success;
    private String email;
    private String accessToken;

    // 정적 팩토리 메서드 - 성공 응답
    public static LoginResponse success(String email, String accessToken) {
        return new LoginResponse(true, email, accessToken);
    }

    // 정적 팩토리 메서드 - 실패 응답
    public static LoginResponse failure() {
        return new LoginResponse(false, null, null);
    }
}
