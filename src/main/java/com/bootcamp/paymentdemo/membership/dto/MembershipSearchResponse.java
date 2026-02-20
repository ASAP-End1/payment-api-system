package com.bootcamp.paymentdemo.membership.dto;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MembershipSearchResponse {

    private final String gradeName;
    private final BigDecimal accRate;
    private final BigDecimal minAmount;

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
