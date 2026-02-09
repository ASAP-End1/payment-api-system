package com.bootcamp.paymentdemo.domain.refund.repository;

import com.bootcamp.paymentdemo.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
