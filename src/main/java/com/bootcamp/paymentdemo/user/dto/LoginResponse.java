package com.bootcamp.paymentdemo.user.dto;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String email;
    private final String accessToken;

    public LoginResponse(String email, String accessToken) {
        this.email = email;
        this.accessToken = accessToken;
    }
}
