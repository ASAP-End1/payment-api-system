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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.dbPaymentId = :dbPaymentId")
    Optional<Payment> findByDbPaymentIdWithLock(@Param("dbPaymentId") String dbPaymentId);

    Optional<Payment> findByDbPaymentId(String dbPaymentId);

}
