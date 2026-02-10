package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.consts.PointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {
    @Query("SELECT p FROM PointTransaction p LEFT JOIN FETCH p.order WHERE p.user.userId = :userId ORDER BY p.createdAt DESC")
    Page<PointTransaction> findPointTransactions(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM PointTransaction p WHERE p.user.userId = :userId")
    BigDecimal calculatePointBalance(@Param("userId") Long userId);

    Optional<PointTransaction> findByOrderIdAndType(Long orderId, PointType pointType);

    @Query("SELECT p FROM PointTransaction p WHERE p.user.userId = :userId AND p.type = 'EARNED' AND " +
            "p.remainingAmount > 0 AND p.expiresAt > CURRENT_DATE ORDER BY p.expiresAt ASC")
    List<PointTransaction> findAvailablePoints(@Param("userId") Long userId);

    @Query("SELECT p FROM PointTransaction p JOIN FETCH p.user WHERE p.type = 'EARNED' AND p.remainingAmount > 0 AND p.expiresAt < CURRENT_DATE")
    List<PointTransaction> findExpiredPoints();
}
