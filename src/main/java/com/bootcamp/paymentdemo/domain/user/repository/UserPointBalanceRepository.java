package com.bootcamp.paymentdemo.domain.user.repository;

import com.bootcamp.paymentdemo.domain.user.entity.UserPointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPointBalanceRepository extends JpaRepository<UserPointBalance, Long> {
    Optional<UserPointBalance> findByUserId(Long userId);
}
