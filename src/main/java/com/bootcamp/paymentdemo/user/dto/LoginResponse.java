package com.bootcamp.paymentdemo.user.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LoginResponse {

    private String email;
    private String accessToken;
}
