package com.bootcamp.paymentdemo.membership.dto;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MembershipSearchResponse {

    private final String gradeName;           // 등급명
    private final BigDecimal accRate;         // 적립률
    private final BigDecimal minAmount;       // 최소 결제 금액

    public MembershipSearchResponse(String gradeName, BigDecimal accRate, BigDecimal minAmount) {
        this.gradeName = gradeName;
        this.accRate = accRate;
        this.minAmount = minAmount;
    }

    public static MembershipSearchResponse from(Membership membership) {
        return new MembershipSearchResponse(
                membership.getGradeName().name() ,
                membership.getAccRate(),
                membership.getMinAmount()
        );
    }
}
