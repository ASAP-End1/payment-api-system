package com.bootcamp.paymentdemo.payment.repository;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
}
