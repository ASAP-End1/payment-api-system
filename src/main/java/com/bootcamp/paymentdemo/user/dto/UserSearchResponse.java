package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UserSearchResponse {
    private final Long userId;
    private final String email;
    private final String name;
    private final String phone;

    private final BigDecimal pointBalance;
    private final BigDecimal totalPaidAmount;
    private final String currentGrade;

    public UserSearchResponse(Long userId, String email, String name, String phone, BigDecimal pointBalance, BigDecimal totalPaidAmount, String currentGrade) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.pointBalance = pointBalance;
        this.totalPaidAmount = totalPaidAmount;
        this.currentGrade = currentGrade;
    }


    public static UserSearchResponse from(User user, UserPointBalance userPointBalance, UserPaidAmount userpaidAmount) {
        return new UserSearchResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getPhoneNumber(),
                userPointBalance.getCurrentPoints(),
                userpaidAmount.getTotalPaidAmount(),
                user.getCurrentGrade().getGradeName().name()
        );
    }

}
