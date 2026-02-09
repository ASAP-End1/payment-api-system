package com.bootcamp.paymentdemo.refund.repository;

import com.bootcamp.paymentdemo.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
