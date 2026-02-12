package com.bootcamp.paymentdemo.payment.repository;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {

    Optional<Payment> findByDbPaymentId(String dbPaymentId);

    // 동시적 환불 문제 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.dbPaymentId = :dbPaymentId")
    Optional<Payment> findByDbPaymentIdWithLock(@Param("dbPaymentId") String dbPaymentId);

}
