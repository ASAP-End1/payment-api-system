package com.bootcamp.paymentdemo.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshResponse {
    // 토큰 갱신 응답 DTO

    private Boolean success;
    private String accessToken;
    private String email;

    // 성공 응답
    public static RefreshResponse success(String accessToken, String email) {
        return new RefreshResponse(true, accessToken, email);
    }

    // 실패 응답
    public static RefreshResponse failure() {
        return new RefreshResponse(false, null, null);
    }
}
