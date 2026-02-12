package com.bootcamp.paymentdemo.user.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SignupResponse {

    private Long userId;
    private String email;
}
