package com.bootcamp.paymentdemo.user.dto;

import lombok.Getter;

@Getter
public class SignupResponse {

    private final Long userId;
    private final String email;

    public SignupResponse(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }
}
