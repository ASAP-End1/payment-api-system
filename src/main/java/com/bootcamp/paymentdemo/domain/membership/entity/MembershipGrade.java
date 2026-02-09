package com.bootcamp.paymentdemo.domain.membership.entity;

import lombok.Getter;

@Getter
public enum MembershipGrade {
    NORMAL("normal")
    , VIP("vip")
    , VVIP("vvip");

    private final String gradeDescription;

    MembershipGrade(String gradeDescription) {
        this.gradeDescription = gradeDescription;
    }
}
