package com.bootcamp.paymentdemo.membership.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "membership_grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @Column(name = "grade_name", length = 20)
    private String gradeName;

    @Column(name = "acc_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal accRate;

    @Column(name = "min_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal minAmount;
}
