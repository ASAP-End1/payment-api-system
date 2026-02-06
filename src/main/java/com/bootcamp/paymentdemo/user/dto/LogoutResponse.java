package com.bootcamp.paymentdemo.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LogoutResponse {

    private Boolean success;
    private String message;

    // 성공 응답
    public static LogoutResponse success() {
        return new LogoutResponse(true, "로그아웃되었습니다.");
    }

    // 커스텀 메시지
    public static LogoutResponse success(String message) {
        return new LogoutResponse(true, message);
    }

    // 실패 응답
    public static LogoutResponse failure(String message) {
        return new LogoutResponse(false, message);
    }
}
