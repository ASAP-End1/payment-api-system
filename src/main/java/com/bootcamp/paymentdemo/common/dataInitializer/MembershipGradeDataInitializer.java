package com.bootcamp.paymentdemo.common.dataInitializer;

// 멤버십 등급 데이터베이스 초기 데이터 설정

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipGradeDataInitializer implements CommandLineRunner {

    private final MembershipRepository gradeRepository;

    @Override
    public void run(String... args) {
        initializeMembershipGrades();
    }

    private void initializeMembershipGrades() {
        if (gradeRepository.count() == 0) {
            log.info("멤버십 등급 데이터 초기화 시작");

            Membership normal = Membership.builder()
                    .gradeName(MembershipGrade.NORMAL)
                    .accRate(new BigDecimal("1.00"))
                    .minAmount(BigDecimal.ZERO)
                    .build();

            Membership vip = Membership.builder()
                    .gradeName(MembershipGrade.VIP)
                    .accRate(new BigDecimal("5.00"))
                    .minAmount(new BigDecimal("50001.00"))
                    .build();

            Membership vvip = Membership.builder()
                    .gradeName(MembershipGrade.VVIP)
                    .accRate(new BigDecimal("10.00"))
                    .minAmount(new BigDecimal("150000.00"))
                    .build();

            gradeRepository.save(normal);
            gradeRepository.save(vip);
            gradeRepository.save(vvip);

            log.info("멤버십 등급 데이터 초기화 완료: NORMAL, VIP, VVIP");
        } else {
            log.info("멤버십 등급 데이터가 이미 존재합니다.");
        }
    }
}
