package com.bootcamp.paymentdemo.user.repository;

import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPaidAmountRepository extends JpaRepository<UserPaidAmount, Long> {
    // userId로 총 결제 금액 조회
    Optional<UserPaidAmount> findByUserId(Long userId);
}
