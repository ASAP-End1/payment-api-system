package com.bootcamp.paymentdemo.membership.dto;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MembershipSearchResponse {

    private String gradeName;           // 등급명
    private BigDecimal accRate;         // 적립률
    private BigDecimal minAmount;       // 최소 결제 금액

    public static MembershipSearchResponse from(Membership membership) {
        return new MembershipSearchResponse(
                membership.getGradeName().name() ,
                membership.getAccRate(),
                membership.getMinAmount()
        );
    }
}
