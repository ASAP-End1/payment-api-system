package com.bootcamp.paymentdemo.payment.repository;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {

    Optional<Payment> findByDbPaymentId(String dbPaymentId);

}
