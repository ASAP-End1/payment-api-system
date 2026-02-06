package com.bootcamp.paymentdemo.user.repository;

import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPointBalanceRepository extends JpaRepository<UserPointBalance, Long> {
}
