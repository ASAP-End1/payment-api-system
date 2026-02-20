package com.bootcamp.paymentdemo.user.repository;

import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPaidAmountRepository extends JpaRepository<UserPaidAmount, Long> {

    Optional<UserPaidAmount> findByUserId(Long userId);
}
