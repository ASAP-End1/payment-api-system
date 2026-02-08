package com.bootcamp.paymentdemo.membership.entity;

import lombok.Getter;

@Getter
public enum MembershipGrade {
    NORMAL("일반 회원")
    , VIP("VIP 회원")
    , VVIP("VVIP 회원");

    private final String gradeDescription;

    MembershipGrade(String gradeDescription) {
        this.gradeDescription = gradeDescription;
    }
}
